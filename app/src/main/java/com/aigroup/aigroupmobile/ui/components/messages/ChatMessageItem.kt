package com.aigroup.aigroupmobile.ui.components.messages

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.LocalTextSpeaker
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageChatError
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageSenderUser
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.readableText
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.utils.common.localDateTime
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.utils.previews.previewModelCode
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.spr.jetpack_loading.components.indicators.BallScaleIndicator
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Clipboard
import compose.icons.fontawesomeicons.solid.Video
import compose.icons.fontawesomeicons.solid.VolumeUp
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatMessageItem(
  modifier: Modifier = Modifier,
  message: MessageChat,
  voiceCode: VoiceCode,
  loading: Boolean = false,
  onRetry: () -> Unit = {},
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val isBot = message.sender?.specific is MessageSenderBot
  val coroutineScope = rememberCoroutineScope()
  val speaker = LocalTextSpeaker.current

  fun copy() {
    // TODO: Support copy multi-media message
    val text = message.readableText
    clipboardManager.setText(AnnotatedString(text))

    // https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
    // Only show a toast for Android 12 and lower.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }
  }

  val containerVerticalPadding = if (isBot) {
    12.dp
  } else {
    20.dp
  }

  var speakerLoading by remember { mutableStateOf(false) }
  fun speak(text: String) = coroutineScope.launch {
    if (speakerLoading) {
      return@launch
    }
    speakerLoading = true

    speaker.speak(text, voiceCode)

    withContext(Dispatchers.Main) {
      speakerLoading = false
    }
  }

  Box(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .background(
        if (isBot) {
          MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
          Color.Transparent
        }
      )
      .padding(horizontal = 10.dp, vertical = containerVerticalPadding)
  ) {
    Column {
      Row {
        if (isBot) {
          val model = (message.sender!!.specific as MessageSenderBot).langBot!!.model

          Box(
            Modifier
              .padding(top = 3.dp)
              .clip(CircleShape)
              .background(model.backColor ?: MaterialTheme.colorScheme.surfaceContainerLowest)
              .size(30.dp)
              .padding(7.dp)
          ) {
            Image(
              painter = painterResource(model.iconId),
              "bot"
            )
          }
        } else {
          UserAvatar(
            size = 30.dp,
            avatar = message.sender!!.userSender!!.userProfile!!.avatar,
            modifier = Modifier.padding(top = 3.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Column {
            Text(
              message.sender!!.specific.username,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Medium
            )
            Text(
              message.id.localDateTime.readableStr,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Column(Modifier.padding(horizontal = 5.dp)) {
        for ((idx, item) in message.parts.withIndex()) {
          val isLast = idx == message.parts.size - 1
          val isMedia = item.specific !is MessageTextItem

          when (val msg = item.specific) {
            is MessageTextItem -> {
              SelectionContainer {
                val style = MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Paragraph)
                CompositionLocalProvider(LocalTextStyle provides style) {
                  RichText(
                    style = RichTextStyle(
                      codeBlockStyle = CodeBlockStyle(
                        modifier = Modifier
                          .clip(MaterialTheme.shapes.small)
                          .background(MaterialTheme.colorScheme.secondaryContainer)
                      ),
                      stringStyle = RichTextStringStyle(
                        codeStyle = SpanStyle(
                          fontWeight = FontWeight.Medium,
                          background = MaterialTheme.colorScheme.secondaryContainer,
                        )
                      )
                    ),
                  ) {
                    Markdown(msg.text)
                  }
                }
              }
            }

            is MessageImageItem -> {
              if (LocalInspectionMode.current) {
                Image(
                  painterResource(R.drawable.avatar_sample),
                  "图片",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .padding(vertical = 10.dp)
                    .height(150.dp)
                    .width(150.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                )
              } else {
                AsyncImage(
                  msg.image!!.url,
                  "图片",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .padding(vertical = 10.dp)
                    .height(150.dp)
                    .width(150.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                )
              }
            }

            is MessageVideoItem -> {
              Box(
                modifier = Modifier
                  .padding(vertical = 10.dp)
              ) {
                if (LocalInspectionMode.current) {
                  Image(
                    painterResource(R.drawable.avatar_sample),
                    "预览",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .height(150.dp)
                      .width(150.dp)
                      .clip(MaterialTheme.shapes.medium)
                      .background(MaterialTheme.colorScheme.secondaryContainer)
                  )
                } else {
                  AsyncImage(
                    ImageRequest.Builder(context)
                      .data(msg.video!!.uri)
                      .decoderFactory { result, options, loader ->
                        VideoFrameDecoder(
                          result.source,
                          options
                        )
                      }
                      .build(),
                    "预览",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .height(150.dp)
                      .width(150.dp)
                      .clip(MaterialTheme.shapes.small)
                      .background(MaterialTheme.colorScheme.secondaryContainer)
                  )
                }

                Icon(
                  FontAwesomeIcons.Solid.Video,
                  "",
                  tint = Color.White,
                  modifier = Modifier
                    .padding(5.dp)
                    .padding(horizontal = 5.dp)
                    .size(17.dp)
                    .align(Alignment.BottomEnd)
                )
              }
            }

            else -> {}
          }

          if (item.error != null) {
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                Icons.Default.Info,
                "",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                  .size(16.dp)
                  .offset(y = (2.5).dp),
              )

              Spacer(modifier = Modifier.width(5.dp))

              Text(
                item.error!!.message,
                style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
                color = MaterialTheme.colorScheme.error,
              )
            }
          }

          Spacer(modifier = Modifier.height(5.dp))

          if (isLast && isMedia && loading) {
            Box(
              Modifier
                .offset(10.dp)
                .padding(vertical = 15.dp)
            ) {
              BallScaleIndicator(
                color = MaterialTheme.colorScheme.primary,
                ballDiameter = 18f,
              )
            }
          }
        }

        val empty =
          message.parts.isEmpty() || (message.parts.first().textItem?.text?.isEmpty() == true)
        if (loading && empty) {
          Box(
            Modifier
              .offset(10.dp)
              .padding(vertical = 15.dp)
          ) {
            BallScaleIndicator(
              color = MaterialTheme.colorScheme.primary,
              ballDiameter = 18f,
            )
          }
        }
      }

      if (isBot) {
        Row(Modifier) {
          IconButton(onClick = {
            copy()
          }) {
            Icon(FontAwesomeIcons.Regular.Clipboard, "", modifier = Modifier.size(17.dp))
          }
          IconButton(
            enabled = !speakerLoading,
            onClick = { speak(message.readableText) },
          ) {
            if (speakerLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(FontAwesomeIcons.Solid.VolumeUp, "", modifier = Modifier.size(17.dp))
            }
          }

          IconButton(
            enabled = !loading,
            onClick = { onRetry() },
          ) {
            Icon(Icons.Default.Refresh, "重试")
          }
        }
      }
    }
  }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
private fun ChatMessageItemPreview() {
  val msg = MessageChat().apply {
    sender = MessageSenderBot("gpt", "gpt bot", LargeLangBot(previewModelCode.fullCode()))
      .createInclusive()
    parts = realmListOf(
      MessageTextItem("Hello World").createInclusive(),
//      MessageImageItem().apply {
//        image = ImageMediaItem(IMAGE)
//      }.createInclusive(),
      MessageVideoItem().apply {
        video = VideoMediaItem(IMAGE, null)
      }.createInclusive(),
      MessageTextItem(LOREM).createInclusive()
    )
  }

  val userMsg = MessageChat().apply {
    sender = MessageSenderUser().apply {
      userProfile = UserProfile().apply {
        username = "jctaoo"
      }
    }
      .createInclusive()
    parts = realmListOf(
      MessageTextItem(LOREM.substring(0, 50)).createInclusive()
    )
  }

  Column(
    Modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    ChatMessageItem(
      message = userMsg,
      voiceCode = AppPreferencesDefaults.defaultVoiceCode,
      modifier = Modifier
        .fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    ChatMessageItem(
      message = msg,
      voiceCode = AppPreferencesDefaults.defaultVoiceCode,
      modifier = Modifier
        .fillMaxWidth()
    )
  }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
private fun ChatMessageItemErrorPreview() {
  val msg = MessageChat().apply {
    sender = MessageSenderBot("gpt", "gpt bot", LargeLangBot(previewModelCode.fullCode()))
      .createInclusive()
    parts = realmListOf(
      MessageTextItem("Hello World").createInclusive(),
//      MessageImageItem().apply {
//        image = ImageMediaItem(IMAGE)
//      }.createInclusive(),
      MessageVideoItem().apply {
        video = VideoMediaItem(IMAGE, null)
      }.createInclusive(),
      MessageTextItem(LOREM).createInclusive(),
      MessageChatData().apply {
        error = MessageChatError("未知错误")
      }
    )
  }

  Column(
    Modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    ChatMessageItem(
      message = msg,
      voiceCode = AppPreferencesDefaults.defaultVoiceCode,
      modifier = Modifier
        .fillMaxWidth()
    )
  }
}

private const val IMAGE =
  "https://images.unsplash.com/photo-1725610588109-71d0def86e19?w=800&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxmZWF0dXJlZC1waG90b3MtZmVlZHw3fHx8ZW58MHx8fHx8"
private const val LOREM =
  """
Hello! I'm an artificial intelligence developed by OpenAI, 

```javascript
console.log("Hello, World!");
```
designed to assist you with information, answer questions,and help with a variety of tasks. How can I help you today?
  """