package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.Prompts
import com.aigroup.aigroupmobile.connect.chat.ai
import com.aigroup.aigroupmobile.connect.images.imageAI
import com.aigroup.aigroupmobile.data.dao.AssistantDao
import com.aigroup.aigroupmobile.data.dao.KnowledgeBaseDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.services.chat.RetrievalAugmentedGeneration
import com.aigroup.aigroupmobile.ui.pages.assistant.AssistantBuilderAvatar
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.viewmodels.SessionSettingsViewModel.Companion
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mongodb.kbson.ObjectId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AssistantBuilderViewModel @Inject constructor(
  val assistantDao: AssistantDao,
  private val userDao: UserDao,
  private val dataStore: DataStore<AppPreferences>,
  private val pathManager: PathManager,
  private val knowledgeBaseDao: KnowledgeBaseDao,
  private val rag: RetrievalAugmentedGeneration,
) : ViewModel() {

  companion object {
    const val TAG = "AssistantBuilderViewModel"

    private val json = Json {
      isLenient = true
      ignoreUnknownKeys = true
    }
  }

  private var built = false
  private val tempKnowledgeBaseID = MutableStateFlow<ObjectId?>(null)

  @OptIn(ExperimentalCoroutinesApi::class)
  val knowledgeBase: Flow<KnowledgeBase?> = tempKnowledgeBaseID.flatMapMerge {
    it?.let { knowledgeBaseDao.getKnowledgeBase(it) } ?: flowOf(null)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val knowledgeDocs: Flow<List<DocumentItem>> = tempKnowledgeBaseID.flatMapMerge {
    it?.let { knowledgeBaseDao.getDocsFromBaseID(it).map { it.list } } ?: flowOf(emptyList())
  }

  /// The list of document ids that are currently being indexed
  private val _indexingDocIds = MutableStateFlow(emptyList<ObjectId>())
  val indexingDocIds: Flow<List<ObjectId>> = _indexingDocIds.asStateFlow()

  // TODO: move state to here

  suspend fun generateAvatar(
    assistantTitle: String?,
    assistantDescription: String?,
  ): AssistantBuilderAvatar? {
    // generate prompt, TODO: use chat coordinator here? and support configure
    val promptModel = AppPreferencesDefaults.defaultModelCode
    val promptAI = dataStore.data.map { it.ai(promptModel.serviceProvider) }.first()

    val helpInfo = """
      The Avatar is generated for a chatbot which:
      - Title: ${if (assistantTitle.isNullOrBlank()) "Not provided" else assistantTitle}
      - Description: ${if (assistantDescription.isNullOrBlank()) "Not provided" else assistantDescription}
    """.trimIndent()
    val promptRequest = chatCompletionRequest {
      messages {
        user { content = Prompts.avatarImagePrompt(helpInfo) }
        model = ModelId(promptModel.code)
      }
    }
    val prompt = promptAI.chatCompletion(promptRequest).choices.first().message.content
    if (prompt.isNullOrBlank()) {
      Log.e(TAG, "Failed to generate avatar image prompt") // TODO: custom exception here
      return null
    }

    // TODO: 支持配置
    val model = AppPreferencesDefaults.defaultAvatarImageModel
    val ai = dataStore.data.map { it.imageAI(model.serviceProvider) }.first()
    val images = ai.createImages(model.model, AppPreferencesDefaults.defaultAvatarImageResolution, prompt, 1)
    if (images.isEmpty()) {
      Log.e(TAG, "Failed to generate avatar image")
      return null
    }

    val avatarImage = images.first()
    Log.i(TAG, "Generated avatar image: $avatarImage")

    val (localUri, _mime) = pathManager.downloadMediaToStorage(Url(avatarImage))

    return AssistantBuilderAvatar.LocalImage(ImageMediaItem(localUri.toString()))
  }

  // TODO: move to Prompts
  // TODO: better code style like dont use suspend

  @OptIn(DelicateCoroutinesApi::class)
  override fun onCleared() {
    super.onCleared()
    Log.i(TAG, "Cleared")

    GlobalScope.launch(Dispatchers.IO) {
      if (tempKnowledgeBaseID.value != null && !built) {
        knowledgeBaseDao.deleteKnowledgeBaseByID(tempKnowledgeBaseID.value!!)
      }
    }
  }

  suspend fun generateDescription(assistantTitle: String): String? {
    // TODO: use chat coordinator here? and support configure
    val promptModel = AppPreferencesDefaults.defaultModelCode
    val promptAI = dataStore.data.map { it.ai(promptModel.serviceProvider) }.first()

    val promptRequest = chatCompletionRequest {
      messages {
        user {
          content = """
          Generate a CHINESE brief description for the assistant bot, this bot is created by user, you can generate a description
          by it's title written by user: "$assistantTitle".
          
          Notes:
          - Do not use markdown or any special characters.
          - A smooth and natural sentence introduction is preferred.
          - About 100 characters is recommended.
        """.trimIndent()
        }
        model = ModelId(promptModel.code)
      }
    }
    val prompt = promptAI.chatCompletion(promptRequest).choices.first().message.content
    if (prompt.isNullOrBlank()) {
      Log.e(TAG, "Failed to generate avatar description prompt") // TODO: custom exception here
      return null
    }

    return prompt
  }

  suspend fun generateStartPrompts(assistantTitle: String, assistantDescription: String): List<String> {
    // TODO: use chat coordinator here? and support configure
    val promptModel = AppPreferencesDefaults.defaultModelCode
    val promptAI = dataStore.data.map { it.ai(promptModel.serviceProvider) }.first()

    try {
      val promptRequest = chatCompletionRequest {
        temperature = 0.1
        messages {
          system {
            // TODO: move to Prompts, using structed response
            content = """
            Given a user customize assistant bot information, produce a a set of 3-5 Chinese statements contains questions
            that users want to ask the assistant robot or tasks that they want the assistant robot to perform.
            These statements are predictions or guidance about possible questions users may ask and should be consistent
            with the responsibilities of the assistant robot.
            User can select one of the statements and send it to the assistant bot driven by the large language model.
            to interact with the assistant robot. Each statements starts with an appropriate emoji symbol. 
            
            The return format is a json array in format of:
            {
              "statements": [
                "...",
                "...",
                "..."
              ]
            }
          """.trimIndent()
          }
          user {
            content = """
          assistant's name: "$assistantTitle".
          assistant's description: "$assistantDescription".
        """.trimIndent()
          }
        }
        model = ModelId(promptModel.code)
        responseFormat = ChatResponseFormat.JsonObject
      }
      val prompt = promptAI.chatCompletion(promptRequest).choices.first().message.content
      if (prompt.isNullOrBlank()) {
        Log.e(TAG, "Failed to generate avatar start prompts") // TODO: custom exception here
        return emptyList()
      }

      Log.i(TAG, "Generated start prompts: $prompt")

      // extract start prompts from json
      val startPrompts = mutableListOf<String>()
      val json = json.parseToJsonElement(prompt).jsonObject
      json["statements"]?.jsonArray?.filter { it.jsonPrimitive.isString }
        ?.forEach { startPrompts.add(it.jsonPrimitive.content) }

      return startPrompts
    } catch (e: Exception) {
      // TODO: show error in ui?
      Log.e(TAG, "Failed to generate start prompts", e)
      return emptyList()
    }
  }

  suspend fun generateRoleDescription(assistantTitle: String, assistantDescription: String): String? {
    // TODO: use chat coordinator here? and support configure
    val promptModel = AppPreferencesDefaults.defaultModelCode
    val promptAI = dataStore.data.map { it.ai(promptModel.serviceProvider) }.first()

    val promptRequest = chatCompletionRequest {
      messages {
        system { content = Prompts.systemPromptGenerator() }
        user {
          content = """
          assistant's name: "$assistantTitle".
          assistant's description: "$assistantDescription".
        """.trimIndent()
        }
        model = ModelId(promptModel.code)
      }
    }

    val prompt = promptAI.chatCompletion(promptRequest).choices.first().message.content
    if (prompt.isNullOrBlank()) {
      Log.e(TAG, "Failed to generate avatar role description prompt") // TODO: custom exception here
      return null
    }

    return prompt
  }

  fun importKnowledgeDocument(doc: DocumentMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      if (tempKnowledgeBaseID.value == null) {
        Log.i(TAG, "Knowledge base not initialized, creating new base")
        val tempBase = knowledgeBaseDao.getTempKnowledgeBase(TAG)

        withContext(Dispatchers.Main) {
          tempKnowledgeBaseID.value = tempBase.id
        }
      }
      // TODO: enhance
      val knowledgeDoc = knowledgeBaseDao.addKnowledgeDocToBase(knowledgeBase.first()!!, doc)
      startIndexDocument(knowledgeDoc)
    }
  }

  fun removeKnowledgeDocument(doc: DocumentItem) {
    viewModelScope.launch(Dispatchers.IO) {
      if (tempKnowledgeBaseID.value == null) {
        Log.i(TAG, "Knowledge base not initialized, cannot remove document")
        return@launch
      }
      // TODO: enhance
      knowledgeBaseDao.deleteKnowledgeDocFromBase(knowledgeBase.first()!!, doc)
    }
  }

  private suspend fun startIndexDocument(doc: DocumentItem) {
    withContext(Dispatchers.IO) {
      _indexingDocIds.value += doc.id
    }

    require(doc.document != null) { "Document is null" }

    withContext(Dispatchers.IO) {
      val knowledgeDoc = rag.indexDocument(doc.document!!)
      Log.i(TAG, "Indexed rag document: ${knowledgeDoc.id}")

      knowledgeBaseDao.updateDocItem(doc) {
        this.knowledgeDocId = knowledgeDoc.id
      }
      Log.i(TAG, "Updated rag doc: ${doc.id} for $doc")
    }

    withContext(Dispatchers.Main) {
      _indexingDocIds.value -= doc.id
    }
  }

  // TODO: refactor enhance code style
  // TODO: check things like index loading
  suspend fun buildAssistant(
    configBuilder: RemoteAssistant.Configuration.Builder,
    metadataBuilder: RemoteAssistant.Metadata.Builder,
    startPrompts: List<String> = emptyList()
  ) {
    val user = userDao.getLocalUser().first().obj
    assert(user != null) { "User not found" }

    val id = UUID.randomUUID().toString()

    val username = user!!.username
    val config = configBuilder.build()
    val metadata = metadataBuilder.author(username).build()
    val remote = RemoteAssistant.Builder()
      .identifier(id)
      .configuration(config).metadata(metadata).startPrompts(startPrompts).build()

    return withContext(Dispatchers.IO) {
      val assistant = assistantDao.cloneRemoteAssistant(remote)

      val base = knowledgeBase.first()
      if (base != null) {
        Log.i(TAG, "Attaching knowledge base to assistant")
        knowledgeBaseDao.attachBaseToAssistant(base, assistant)
      }

      built = true
    }
  }

}