package com.aigroup.aigroupmobile.services.chat.plugins.builtin

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.aallam.openai.api.chat.ToolBuilder
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginDescription
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScope
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginUpdateScope
import com.aigroup.aigroupmobile.utils.network.createHttpClient
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val SERPER_GOOGLE_API = "https://google.serper.dev/search"

private val SUMMARY_PROMPT = { query: String, searchResult: String ->
  """
SEARCH_QUERY:
{$query}

SEARCH_RESULT:
$searchResult
  
INSTRUCTION:
You are a assistant to summary topics and important information from the given search result from google.
Keep your answer ground in the facts of the SEARCH_RESULT, you can reference the SEARCH_QUERY to provide the answer.
Answer in CHINESE.

DEMO:
- ${appStringResource(R.string.label_prompt_example_online_search)}
""".trim()
}

class SerperGooglePlugin : ChatPlugin() {
  companion object : ChatPluginDescription<SerperGooglePlugin> {
    const val TAG = "SerperPlugin"

    override val name = "serper-search"
    override val displayName: String
      @Composable
      get() = stringResource(R.string.label_plugin_name_serper_search)
    override val icon = @Composable { ImageVector.vectorResource(R.drawable.ic_search_icon) }
    override val tintColor: Color = MaterialColors.Blue[100]
    override val iconColor: Color = MaterialColors.Blue[900]

    override val builder: ToolBuilder.() -> Unit = {
      function(
        name = name,
        description = "Search information by given query on google",
      ) {
        put("type", "object")
        putJsonObject("properties") {
          putJsonObject("query") {
            put("type", "string")
            put("description", "The query to search on google")
          }
        }
        putJsonArray("required") {
          add("query")
        }
      }
    }

    override fun create(): SerperGooglePlugin {
      return SerperGooglePlugin()
    }
  }

  private val client = createHttpClient()

  private suspend fun ChatPluginRunScope.search(query: String): String {
    if (!userPreferences.serviceToken.hasSerper()) {
      throw Exception("Serper service token is not set")
    }

    val key = userPreferences.serviceToken.serper

    val response = client.post(SERPER_GOOGLE_API) {
      contentType(ContentType.Application.Json)
      headers {
        append("X-API-KEY", key)
      }
      setBody(buildJsonObject {
        put("q", query)
      })
    }

    if (response.status == HttpStatusCode.OK) {
      return response.bodyAsText()
    } else {
      val msg = response.bodyAsText()
      throw Exception("Failed to search $msg")
    }
  }

  override suspend fun execute(
    args: JsonObject,
    botMessage: MessageChat,
    run: suspend (suspend ChatPluginRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ) {
    val query = args.getValue("query").jsonPrimitive.content
    Log.d(TAG, "execute: $query")

    run {
      val result = search(query)

      updater {
        var responseText = ""

        val request = chatCompletionRequest {
          temperature = 0.1
          this.model = ModelId(userModel.toString())
          this.messages {
            user {
              content = SUMMARY_PROMPT(query, result)
            }
          }
        }

        // TODO: 迁移到 ChatPlugin 公共行为
        if (userModel.supportStream) {
          userAI.chatCompletions(request).collect {
            val content = it.choices.firstOrNull()?.delta?.content
            if (content?.isNotEmpty() == true) {
              responseText += content
              chatDao.updateMessageLastTextOrCreate(botMessage) {
                this.text += content
              }
            }
          }
        } else {
          val completion = userAI.chatCompletion(request)
          val content = completion.choices.firstOrNull()?.message?.content
          if (content?.isNotEmpty() == true) {
            responseText = content
            chatDao.updateMessageLastTextOrCreate(botMessage) {
              this.text += content
            }
          }
        }

        Log.i(TAG, "Search Summary: $responseText (query: $query; model: ${userModel.fullCode()})")
      }
    }
  }
}