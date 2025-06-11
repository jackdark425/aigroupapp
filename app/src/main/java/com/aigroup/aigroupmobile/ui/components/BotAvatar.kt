package com.aigroup.aigroupmobile.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.widget.EmojiTextView
import coil.compose.AsyncImage
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import io.ktor.http.Url
import io.realm.kotlin.ext.realmListOf

private fun calculatePaddingFromSize(size: Dp): Dp {
  // base: 30 size --> 7 padding
  return size * 0.23f
}

private fun calculateEmojiPaddingFromSize(size: Dp): Dp {
  when (size.value) {
    in 40f..Float.MAX_VALUE -> return size * 0.13f
    else -> return 0.dp
  }
}

private fun calculateEmojiSizeFromSize(size: Dp): Float {
  // base: 30 size --> 20sp
  return size.value * 0.57f
}

/**
 * Avatar by model
 */
@Deprecated("Use BotAvatar instead", ReplaceWith("BotAvatar(bot, size, modifier)"))
@Composable
fun BotAvatar(
  model: ModelCode?, // TODO: make it required
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  val padding by remember { derivedStateOf { calculatePaddingFromSize(size) } }

  Box(
    modifier
      .clip(shape)
      .background(model?.backColor ?: MaterialTheme.colorScheme.surfaceContainerLowest)
      .size(size)
      .padding(padding)
  ) {
    if (model != null) {
      Image(
        painter = painterResource(model.iconId), "bot"
      )
    }
  }
}

/**
 * Avatar by emoji TODO: private it
 */
@Composable
fun BotAvatarEmoji(
  emoji: String,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  val padding by remember { derivedStateOf { calculateEmojiPaddingFromSize(size) } }

  Box(
    modifier
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .size(size)
      .padding(padding),
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      factory = { context ->
        EmojiTextView(context).apply {
          textSize = calculateEmojiSizeFromSize(size)
          text = emoji
        }
      },
      modifier = Modifier
    )
  }
}

/**
 * Avatar by resources
 */
@Composable
private fun BotAvatar(
  @DrawableRes resId: Int,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  Image(
    painter = painterResource(resId), "bot",
    contentScale = ContentScale.Crop,
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceContainer)
  )
}

/**
 * Avatar by ImageMediaItem
 */
@Composable
fun BotAvatar(
  mediaItem: ImageMediaItem?,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  if (LocalInspectionMode.current || mediaItem == null) {
    Image(
      painter = painterResource(R.drawable.user_default_avatar),
      contentDescription = "avatar",
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
    )
  } else {
    AsyncImage(
      mediaItem.url,
      "avatar",
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
    )
  }
}

// TODO: replace all to botAvatar
@Composable
fun BotAvatar(
  bot: MessageSenderBot,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  val assistant = bot.assistant
  when {
    assistant?.avatar != null -> {
      BotAvatar(assistant.avatar, size, modifier, shape)
    }

    assistant?.avatarEmoji != null -> {
      BotAvatarEmoji(assistant.avatarEmoji!!, size, modifier, shape)
    }

    else -> {
      BotAvatar(bot.langBot?.model, size, modifier, shape)
    }
  }
}

@Composable
fun BotAvatar(
  bot: RemoteAssistant,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  when {
    bot.metadata.avatar != null -> {
      BotAvatar(
        bot.metadata.avatar,
        size, modifier,
        shape
      )
    }

    bot.metadata.avatarLink != null -> {
      BotAvatar(
        // TODO remove this tric to use remote link on ImageMediaItem
        ImageMediaItem(bot.metadata.avatarLink),
        size, modifier,
        shape
      )
    }

    bot.metadata.avatarEmoji != null -> {
      BotAvatarEmoji(bot.metadata.avatarEmoji, size, modifier, shape)
    }

    else -> {
      error("No avatar found")
    }
  }
}

@Composable
fun BotAvatar(
  bot: BotAssistant,
  size: Dp = 30.dp,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  when {
    bot.avatar != null -> {
      BotAvatar(bot.avatar, size, modifier, shape)
    }

    bot.avatarEmoji != null -> {
      BotAvatarEmoji(bot.avatarEmoji!!, size, modifier, shape)
    }

    else -> {
      error("No avatar found")
    }
  }
}

@Preview
@Composable
private fun BotAvatarPreview() {
  val assistant = BotAssistant().apply {
    avatar = ImageMediaItem()
    tags = realmListOf("AI", "Assistant")
    presetsPrompt = "Your are a great assistant!"
  }
  val bot = MessageSenderBot(
    "Writer Assistant",
    "Your personal AI assistant to help you with tasks.",
    null,
    assistant
  )

  AIGroupAppTheme {
    BotAvatar(bot)
  }
}

@Preview
@Composable
private fun BotAvatarEmojiPreview() {
  AIGroupAppTheme {
    BotAvatarEmoji("🤖")
  }
}