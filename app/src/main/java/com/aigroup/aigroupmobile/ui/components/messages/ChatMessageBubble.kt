@file:OptIn(ExperimentalFoundationApi::class)

package com.aigroup.aigroupmobile.ui.components.messages

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.LocalTextSpeaker
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageChatError
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageSenderInclusive
import com.aigroup.aigroupmobile.data.models.MessageSenderUser
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.MessageDocItem
import com.aigroup.aigroupmobile.data.models.readableText
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.services.TextSpeaker
import com.aigroup.aigroupmobile.services.chat.plugins.BuiltInPlugins
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.CodeView
import com.aigroup.aigroupmobile.ui.components.CommonMarkText
import com.aigroup.aigroupmobile.ui.components.EditingArea
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.services.documents.DocumentPreviewer
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.getCurrentDesignedLocale
import com.aigroup.aigroupmobile.utils.common.localDateTime
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.utils.previews.previewModelCode
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.spr.jetpack_loading.components.indicators.BallScaleIndicator
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Video
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private sealed class MarkTextSplit(open val content: String) {
  data class Text(override val content: String) : MarkTextSplit(content)
  data class Code(override val content: String, val language: String, val collapsed: Boolean = false) :
    MarkTextSplit(content)
}

private fun calculateMarkdownSplits(text: String): List<MarkTextSplit> {
  val splits = mutableListOf<MarkTextSplit>()

  var textBuffer = ""
  var language = ""
  var codeBuffer = ""
  var inSideCodeBlock = false

  text.lineSequence().forEach {
    if (it.startsWith("```")) {
      if (inSideCodeBlock) {
        splits.add(MarkTextSplit.Code(codeBuffer, language))
        codeBuffer = ""
        inSideCodeBlock = false
      } else {
        splits.add(MarkTextSplit.Text(textBuffer))
        textBuffer = ""
        inSideCodeBlock = true
        language = it.removePrefix("```")
      }
    } else {
      if (inSideCodeBlock) {
        codeBuffer += it + "\n"
      } else {
        textBuffer += it + "\n"
      }
    }
  }

  if (inSideCodeBlock) {
    splits.add(MarkTextSplit.Code(codeBuffer, language))
  } else if (textBuffer.isNotEmpty()) {
    splits.add(MarkTextSplit.Text(textBuffer))
  }

  return splits
}

@Composable
private fun Avatar(sender: MessageSenderInclusive) {
  val isBot = sender.specific is MessageSenderBot
  if (isBot) {
    BotAvatar(sender.specific as MessageSenderBot, size = 30.dp, modifier = Modifier.padding(top = 3.dp))
  } else {
    UserAvatar(size = 30.dp, avatar = sender.userSender!!.userProfile!!.avatar, modifier = Modifier.padding(top = 3.dp))
  }
}

@Composable
fun ChatMessageBubbleBase(
  modifier: Modifier = Modifier,
  message: MessageChat,
  showLoadingBadge: Boolean,
  content: @Composable ColumnScope.() -> Unit = {}
) {
  val isBot = message.sender?.specific is MessageSenderBot

  Box(
    modifier = modifier.fillMaxWidth()
  ) {
    Column {
      Row(
        modifier = Modifier,
        verticalAlignment = Alignment.Bottom
      ) {
        if (isBot) {
          BadgedBox(badge = {
            androidx.compose.animation.AnimatedVisibility(showLoadingBadge) {
              Badge(
                containerColor = AppCustomTheme.colorScheme.primaryAction,
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(8.dp),
                  strokeWidth = 1.dp,
                  color = AppCustomTheme.colorScheme.onPrimaryAction,
                )
              }
            }
          }) {
            Avatar(message.sender!!)
          }
          Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
          horizontalAlignment = if (isBot) Alignment.Start else Alignment.End,
          modifier = Modifier.weight(1f)
        ) {
          content()
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = if (isBot) 45.dp else 10.dp),
        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
      ) {
        DisableSelection {
          Text(
            message.id.localDateTime.readableStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }
    }
  }
}

@Composable
fun ChatMessageBubble(
  modifier: Modifier = Modifier,
  message: MessageChat,
  part: MessageChatData,
  voiceCode: VoiceCode,
  loading: Boolean = false,
  onRetry: () -> Unit = {},
  onEdit: (String) -> Unit = {},

  onPreviewMedia: (MediaItem) -> Unit = {},
  onTranslateContent: (part: MessageTextItem, locale: Locale) -> Unit = { _, _ ->},
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isBot = message.sender?.specific is MessageSenderBot

  var showMenu by remember { mutableStateOf(false) }
  var showEditModal by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current

  fun copy() {
    // TODO: Support copy multi-media message
    val text = if (part.error == null) message.readableText else part.error!!.message
    clipboardManager.setText(AnnotatedString(text))

    // https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
    // Only show a toast for Android 12 and lower.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }
  }

  fun edit(text: String) {
//    println("edit text to: $text")
    onEdit(text)
  }

  val speaker = LocalTextSpeaker.current

  var speakerLoading by remember { mutableStateOf(false) }
  var translateLoading by remember { mutableStateOf(false) }

  fun speak(text: String) = coroutineScope.launch {
    if (speakerLoading) {
      return@launch
    }
    speakerLoading = true

    speaker.speak(text, voiceCode)

    withContext(Dispatchers.Main) {
      speakerLoading = false
      showMenu = false
    }
  }

  fun translate(part: MessageTextItem) = coroutineScope.launch {
    if (translateLoading) {
      return@launch
    }
    translateLoading = true

    // translate ...
    // TODO: 这是 app 支持的用户语言，考虑 app 不支持的语言的情况
    onTranslateContent(part, context.getCurrentDesignedLocale())

    withContext(Dispatchers.Main) {
      translateLoading = false
      showMenu = false
    }
  }

  val empty = part.textItem?.text?.isEmpty() == true

  ChatMessageBubbleBase(
    message = message,
    modifier = modifier,
    showLoadingBadge = loading && !empty
  ) {
    val basePadding =
      if (part.specific is MessageImageItem || part.specific is MessageVideoItem) 5.dp else 9.dp
    val isTextItem = part.specific is MessageTextItem

    val shouldShowMenu = showMenu && (isTextItem || part.error != null) && !loading
    val boxScale = if (LocalInspectionMode.current) {
      1f
    } else {
      animateFloatAsState(
        targetValue = if (shouldShowMenu) 0.95f else 1f,
        animationSpec = tween(150)
      ).value
    }

    BoxWithConstraints(
      modifier = Modifier
        .graphicsLayer {
          scaleX = boxScale
          scaleY = boxScale
          transformOrigin = TransformOrigin(if (isBot) 0f else 1f, 0.5f) // TODO: rtl
        }
        .clip(MaterialTheme.shapes.medium)
        .clickable {
          // TODO: hide selection area, and use scope.baseClickBehavior !!
          focusManager.clearFocus()

          if (part.error != null) {
            showMenu = !showMenu
          } else {
            when (val specific = part.specific) {
              is MessageTextItem -> {
                showMenu = !showMenu
              }

              is MessageImageItem -> {
                onPreviewMedia(specific.image!!)
              }

              is MessageVideoItem -> {
                onPreviewMedia(specific.video!!)
              }

              is MessageDocItem -> {
                onPreviewMedia(specific.document!!)
              }

              else -> {
              }
            }
          }
        }
        .background(
          if (shouldShowMenu)
            MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.8f)
          else
            MaterialTheme.colorScheme.surfaceContainerLowest
        )
        .padding(basePadding)
    ) {
      val fraction = if (isBot) 0.95f else 0.85f

      Box(
        Modifier
          .widthIn(30.dp, maxWidth * fraction)
          .heightIn(min = 20.dp),
      ) {
        if (loading && empty) {
          Box(
            Modifier
              .offset(15.dp)
              .padding(vertical = 12.dp)
              .width(30.dp)
          ) {
            BallScaleIndicator(
              color = MaterialTheme.colorScheme.primary,
              ballDiameter = 18f,
            )
          }
        }

        if (part.error != null) {
          // TODO: 优化错误信息显示
          CompositionLocalProvider(LocalContentColor provides MaterialColors.Red[400]) {
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                Icons.Outlined.Info,
                "",
                modifier = Modifier
                  .size(16.dp)
                  .offset(y = (2.5).dp),
              )

              Spacer(modifier = Modifier.width(5.dp))

              Column {
                Text(
                  part.error!!.message,
                  style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
                )
                // TODO: retry button here
              }
            }
          }
        } else {
          when (val msg = part.specific) {
            is MessageTextItem -> {
              val style = MaterialTheme.typography.bodyMedium
                .copy(lineBreak = LineBreak.Paragraph)
                .copy(color = MaterialTheme.colorScheme.onSurface)
                .copy(lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6f)

              CompositionLocalProvider(LocalTextStyle provides style) {
                when {
                  !isBot -> {
                    Text(msg.text)
                  }

                  loading -> {
                    CommonMarkText(msg.text)
                  }

                  else -> {
                    val textSplits by remember(msg.text) {
                      derivedStateOf {
                        calculateMarkdownSplits(msg.text)
                      }
                    }

                    Column {
                      for ((idx, split) in textSplits.withIndex()) {
                        when (split) {
                          is MarkTextSplit.Text -> {
                            CommonMarkText(split.content)
                          }

                          is MarkTextSplit.Code -> {
                            CodeView(split.content, split.language)
                          }
                        }
                        if (idx != textSplits.size - 1) {
                          Spacer(modifier = Modifier.height(10.dp))
                        }
                      }
                      msg.translatedText?.let {
                        // create devider
                        HorizontalDivider(
                          modifier = Modifier.padding(vertical = 5.dp),
                          color = MaterialTheme.colorScheme.secondaryContainer
                        )
                        Text(
                          it,
                          style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                      }
                    }
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
                    .height(150.dp)
                    .width(150.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                )
              } else {
                // TODO: using TransformImageView
                AsyncImage(
                  msg.image!!.url,
                  "图片",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
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
                  if (msg.video?.snapshot != null) {
                    AsyncImage(
                      msg.video!!.snapshot!!.url,
                      "视频预览",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .height(150.dp)
                        .width(150.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                  } else {
                    // 无 snapshot
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

            is MessageDocItem -> {
              val doc = msg.document!!
              DocumentPreviewer(doc)
            }

            else -> {}
          }
        }
      }
    }

    if (shouldShowMenu) {
      Box(Modifier.align(if (isBot) Alignment.Start else Alignment.End)) {
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          shadowElevation = 5.dp,
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          shape = MaterialTheme.shapes.medium,
        ) {
          AppDropdownMenuItem(
            text = { Text(stringResource(R.string.label_menu_copy)) },
            leadingIcon = {
              Icon(
                ImageVector.vectorResource(R.drawable.ic_copy_lite_icon_legacy),
                "",
                modifier = Modifier.size(20.dp)
              )
            },
            onClick = {
              copy()
              showMenu = false
            },
            itemMinHeight = 32.dp
          )
          if (isBot) {
            if (part.error == null) {
              AppDropdownMenuItem(
                text = { Text(stringResource(R.string.label_menu_tts)) },
                leadingIcon = {
                  if (speakerLoading) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(11.dp),
                      strokeWidth = 3.dp,
                      color = AppCustomTheme.colorScheme.primaryAction
                    )
                  } else {
                    Icon(
                      ImageVector.vectorResource(R.drawable.ic_play_icon),
                      "",
                      modifier = Modifier
                        .size(20.dp)
                        .scale(0.75f)
                    )
                  }
                },
                onClick = {
                  speak(part.textItem!!.text)
                },
                itemMinHeight = 32.dp
              )

              if (part.specific is MessageTextItem)
                AppDropdownMenuItem(
                  text = { Text(stringResource(R.string.label_menu_translate)) },
                  leadingIcon = {
                    if (translateLoading) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        strokeWidth = 3.dp,
                        color = AppCustomTheme.colorScheme.primaryAction
                      )
                    } else {
                      Icon(
                        ImageVector.vectorResource(R.drawable.ic_translate_icon),
                        "",
                        modifier = Modifier
                          .size(20.dp)
                          .scale(0.75f)
                      )
                    }
                  },
                  onClick = {
                    (part.specific as? MessageTextItem)?.let {
                      translate(it)
                    }
                  },
                  itemMinHeight = 32.dp
                )
            }
            AppDropdownMenuItem(
              text = { Text(stringResource(R.string.label_menu_regenerate)) },
              leadingIcon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_retry_icon),
                  "",
                  modifier = Modifier.size(18.dp)
                )
              },
              onClick = {
                onRetry()
                showMenu = false
              },
              itemMinHeight = 32.dp
            )

            if (message.pluginId != null) {
              val plugin = BuiltInPlugins.plugins.firstOrNull { it.name == message.pluginId }
              if (plugin != null) {
                AppDropdownMenuItem(
                  text = {
                    Text(
                      stringResource(R.string.label_menu_plugin_generated, plugin.displayName),
                      color = AppCustomTheme.colorScheme.secondaryLabel
                    )
                  },
                  leadingIcon = {
                    Icon(
                      plugin.icon(),
                      "",
                      tint = AppCustomTheme.colorScheme.secondaryLabel,
                      modifier = Modifier.size(13.dp)
                    )
                  },
                  enabled = false,
                  onClick = {},
                  itemMinHeight = 32.dp
                )
              }
            }
          } else {
            AppDropdownMenuItem(
              text = { Text(stringResource(R.string.label_menu_edit_msg)) },
              leadingIcon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_edit_text_icon),
                  "",
                  modifier = Modifier.size(18.dp)
                )
              },
              onClick = {
                showMenu = false
                showEditModal = true
              },
              itemMinHeight = 32.dp
            )
          }
        }
      }
    }
  }

  if (showEditModal) {
    EditingArea(
      initialText = part.textItem?.text ?: "",
      onDismissRequest = { showEditModal = false }
    ) {
      edit(it)
    }
  }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
private fun ChatMessageBubblePreview() {
  val msg = MessageChat().apply {
    sender = MessageSenderBot("gpt", "gpt bot", LargeLangBot(previewModelCode.fullCode())).createInclusive()
    parts = realmListOf(
      MessageTextItem("Hello World").apply {
        this.translatedText = "你好，世界"
      }.createInclusive(),
      MessageImageItem().apply {
        image = ImageMediaItem(IMAGE)
      }.createInclusive(),
      MessageVideoItem().apply {
        video = VideoMediaItem(IMAGE, null)
      }.createInclusive(),
      MessageDocItem().apply {
        document = DocumentMediaItem("/sample/uri", "text/plain", "sample.txt")
      }.createInclusive(),
      MessageTextItem(LOREM).createInclusive()
    )
  }

  val userMsg = MessageChat().apply {
    sender = MessageSenderUser().apply {
      userProfile = UserProfile().apply {
        username = "jctaoo"
      }
    }.createInclusive()
    parts = realmListOf(
      MessageTextItem(LOREM.substring(0, 50)).createInclusive()
    )
  }

  val ctx = LocalContext.current
  val textSpeaker = remember { TextSpeaker(ctx) }

  AIGroupAppTheme {
    CompositionLocalProvider(LocalTextSpeaker provides  textSpeaker) {
      Column(
        Modifier
          .fillMaxSize()
          .background(AppCustomTheme.colorScheme.groupedBackground)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
        ChatMessageBubble(
          message = userMsg, voiceCode = AppPreferencesDefaults.defaultVoiceCode, part = userMsg.parts[0]
        )
        Spacer(modifier = Modifier.height(10.dp))
        for (part in msg.parts) {
          ChatMessageBubble(
            message = msg, voiceCode = AppPreferencesDefaults.defaultVoiceCode, part = part
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }
  }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
private fun ChatMessageBubbleErrorPreview() {
  val msg = MessageChat().apply {
    sender = MessageSenderBot("gpt", "gpt bot", LargeLangBot(previewModelCode.fullCode())).createInclusive()
    parts = realmListOf(MessageTextItem("Hello World").createInclusive(),
      MessageVideoItem().apply {
        video = VideoMediaItem(IMAGE, null)
      }.createInclusive(), MessageTextItem(LOREM).createInclusive(), MessageChatData().apply {
        error = MessageChatError("未知错误")
      })
  }

  val ctx = LocalContext.current
  val textSpeaker = remember { TextSpeaker(ctx) }

  AIGroupAppTheme {
      CompositionLocalProvider(LocalTextSpeaker provides  textSpeaker) {
        Column(
          Modifier
            .fillMaxSize()
            .background(AppCustomTheme.colorScheme.groupedBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
          for (part in msg.parts) {
            ChatMessageBubble(
              message = msg, voiceCode = AppPreferencesDefaults.defaultVoiceCode, part = part
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
        }
      }
  }
}

@Preview(showSystemUi = true, device = "id:pixel_6")
@Composable
private fun ChatMessageBubbleLoadingPreview() {
  val msg = MessageChat().apply {
    sender = MessageSenderBot("gpt", "gpt bot", LargeLangBot(previewModelCode.fullCode())).createInclusive()
    parts = realmListOf(MessageTextItem("Hello World").createInclusive(),
      MessageVideoItem().apply {
        video = VideoMediaItem(IMAGE, null)
      }.createInclusive(), MessageTextItem(LOREM).createInclusive(), MessageChatData().apply {
        textItem = MessageTextItem("")
      })
  }

  val ctx = LocalContext.current
  val textSpeaker = remember { TextSpeaker(ctx) }

  AIGroupAppTheme {
    CompositionLocalProvider(LocalTextSpeaker provides  textSpeaker) {
      Column(
        Modifier
          .fillMaxSize()
          .background(AppCustomTheme.colorScheme.groupedBackground)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
        for (part in msg.parts) {
          ChatMessageBubble(
            message = msg, voiceCode = AppPreferencesDefaults.defaultVoiceCode, part = part, loading = true
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }
  }
}


private const val IMAGE =
  "https://images.unsplash.com/photo-1725610588109-71d0def86e19?w=800&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxmZWF0dXJlZC1waG90b3MtZmVlZHw3fHx8ZW58MHx8fHx8"
private const val LOREM = """Hello! I'm an artificial intelligence developed by OpenAI, 

```javascript
console.log("Hello, World!");
```
designed to assist you with information, answer questions,and help with a variety of tasks. How can I help you today?
  """