package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Video

interface MediaItemViewInteractionScope {
  fun toggleMenu()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaItemView(
  item: MediaItem,
  onClick: (MediaItemViewInteractionScope.() -> Unit)? = null,
  onLongPress: (MediaItemViewInteractionScope.() -> Unit)? = null,
  modifier: Modifier = Modifier,

  // TODO: try context receivers
  menuContent: @Composable ColumnScope.(MediaItemViewInteractionScope) -> Unit = {}
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }

  val interactionScope = object : MediaItemViewInteractionScope {
    override fun toggleMenu() {
      showMenu = !showMenu
    }
  }

  Box(
    modifier
      .clip(MaterialTheme.shapes.medium)
      .combinedClickable(
        enabled = onClick != null,
        onClick = {
          onClick?.invoke(interactionScope)
        },
        onLongClick = {
          onLongPress?.invoke(interactionScope)
        }
      )
  ) {
    when (item) {
      is ImageMediaItem -> {
        if (LocalInspectionMode.current) {
          Image(
            painterResource(R.drawable.avatar_sample),
            "图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .padding(2.dp)
              .background(MaterialTheme.colorScheme.secondaryContainer)
          )
        } else {
          // TODO: using TransformImageView
          AsyncImage(
            item.url,
            "图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .padding(2.dp)
              .background(MaterialTheme.colorScheme.secondaryContainer)
          )
        }
      }

      is VideoMediaItem -> {
        Box(
          modifier = Modifier
        ) {
          if (LocalInspectionMode.current) {
            Image(
              painterResource(R.drawable.avatar_sample),
              "预览",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer)
            )
          } else {
            if (item.snapshot != null) {
              AsyncImage(
                item.snapshot!!.url,
                "视频预览",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxSize()
                  .padding(2.dp)
                  .background(MaterialTheme.colorScheme.secondaryContainer)
              )
            } else {
              AsyncImage(
                ImageRequest.Builder(context)
                  .data(item.url)
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
                  .fillMaxSize()
                  .padding(2.dp)
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

      else -> {
        // TODO: render custom media
      }
    }

    if (showMenu) {
      DropdownMenu(
        expanded = true,
        onDismissRequest = { showMenu = false },
        content = { menuContent.invoke(this, interactionScope) },
        shadowElevation = 5.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
      )
    }
  }
}

@Composable
fun <T> MediaGrid(
  items: List<T>,
  mediaItem: (T) -> MediaItem,
  onClick: (MediaItemViewInteractionScope.(T) -> Unit) = {},
  onLongPress: (MediaItemViewInteractionScope.(T) -> Unit) = {},
  modifier: Modifier,
  menuContent: @Composable ColumnScope.(T, MediaItemViewInteractionScope) -> Unit = { a, b -> }
) {
  require(items.size in 1..4) { "Media items count should be 1-4" }

  // TODO: better coding solution

  ConstraintLayout(modifier) {
    when (items.count()) {
      1 -> {
        val item = items.first()
        Box(Modifier.fillMaxSize()) {
          MediaItemView(
            mediaItem(item),
            { onClick(item) },
            { onLongPress(item) },
            menuContent = { scope ->
              menuContent(item, scope)
            }
          )
        }
      }

      2 -> {
        val item1 = items[0]
        val item2 = items[1]
        val (item1Ref, item2Ref) = createRefs()

        MediaItemView(
          mediaItem(item1),
          { onClick(item1) },
          { onLongPress(item1) },
          Modifier.constrainAs(item1Ref) {
            start.linkTo(parent.start)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item1, it)
        }
        MediaItemView(
          mediaItem(item2),
          { onClick(item2) },
          { onLongPress(item2) },
          Modifier.constrainAs(item2Ref) {
            start.linkTo(item1Ref.end)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item2, it)
        }
      }

      3 -> {
        val item1 = items[0]
        val item2 = items[1]
        val item3 = items[2]
        val (item1Ref, item2Ref, item3Ref) = createRefs()

        MediaItemView(
          mediaItem(item1),
          { onClick(item1) },
          { onLongPress(item1) },
          Modifier.constrainAs(item1Ref) {
            start.linkTo(parent.start)
            height = Dimension.percent(0.5f)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item1, it)
        }
        MediaItemView(
          mediaItem(item2),
          { onClick(item2) },
          { onLongPress(item2) },
          Modifier.constrainAs(item2Ref) {
            start.linkTo(parent.start)
            top.linkTo(item1Ref.bottom)
            height = Dimension.percent(0.5f)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item2, it)
        }
        MediaItemView(
          mediaItem(item3),
          { onClick(item3) },
          { onLongPress(item3) },
          Modifier.constrainAs(item3Ref) {
            start.linkTo(item1Ref.end)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item3, it)
        }
      }

      4 -> {
        val item1 = items[0]
        val item2 = items[1]
        val item3 = items[2]
        val item4 = items[3]
        val (item1Ref, item2Ref, item3Ref, item4Ref) = createRefs()

        MediaItemView(
          mediaItem(item1),
          { onClick(item1) },
          { onLongPress(item1) },
          Modifier.constrainAs(item1Ref) {
            start.linkTo(parent.start)
            height = Dimension.percent(0.5f)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item1, it)
        }
        MediaItemView(
          mediaItem(item2),
          { onClick(item2) },
          { onLongPress(item2) },
          Modifier.constrainAs(item2Ref) {
            start.linkTo(item1Ref.end)
            height = Dimension.percent(0.5f)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item2, it)
        }
        MediaItemView(
          mediaItem(item3),
          { onClick(item3) },
          { onLongPress(item3) },
          Modifier.constrainAs(item3Ref) {
            start.linkTo(parent.start)
            top.linkTo(item1Ref.bottom)
            height = Dimension.percent(0.5f)
            width = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item3, it)
        }
        MediaItemView(
          mediaItem(item4),
          { onClick(item4) },
          { onLongPress(item4) },
          Modifier.constrainAs(item4Ref) {
            start.linkTo(item3Ref.end)
            top.linkTo(item2Ref.bottom)
            width = Dimension.percent(0.5f)
            height = Dimension.percent(0.5f)
          }
        ) {
          menuContent(item4, it)
        }
      }
    }
  }
}

@Composable
fun MediaGrid(
  items: List<MediaItem>,
  onClick: (MediaItemViewInteractionScope.(MediaItem) -> Unit) = {},
  onLongPress: (MediaItemViewInteractionScope.(MediaItem) -> Unit) = {},
  modifier: Modifier,
  menuContent: @Composable ColumnScope.(MediaItem, MediaItemViewInteractionScope) -> Unit = { a, b -> }
) {
  MediaGrid(
    items,
    { it },
    onClick,
    onLongPress,
    modifier,
    menuContent
  )
}

private val ImageLinks = listOf(
  "https://unsplash.com/photos/a-mountain-with-a-fire-in-the-middle-of-it--TE9BIVa1Zo",
  "https://unsplash.com/photos/a-view-of-the-grand-canyon-from-a-plane-EuTlfLqYWp8",
  "https://unsplash.com/photos/a-couple-of-people-standing-on-top-of-a-sandy-beach-bp_sR7TIo9s",
//  "https://unsplash.com/photos/a-view-of-the-grand-canyon-from-a-plane-EuTlfLqYWp8",
)

@Preview(showBackground = true)
@Composable
fun PreviewMediaGrid() {
  AIGroupAppTheme {
    ConstraintLayout {
      MediaGrid(
        items = ImageLinks.map { ImageMediaItem(it) },
        modifier = Modifier.size(
          width = 500.dp,
          height = 300.dp
        )
      )
    }
  }
}