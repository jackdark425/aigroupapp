@file:OptIn(
  ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
)

package com.aigroup.aigroupmobile.ui.components

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.GenericMediaItem
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.utils.common.filename
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.utils.system.PermissionUtils
import com.aigroup.aigroupmobile.utils.system.VideoUtils
import com.aigroup.aigroupmobile.utils.previews.rememberMultiplePermissionsStateSafe
import com.aigroup.aigroupmobile.utils.previews.rememberPermissionStateSafe
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MediaSelector {

  enum class MediaType {
    Image,
    Video,
    ImageAndVideo,
  }

  private const val TAG = "MediaSelector"

  // TODO: tap to preview media (including video)
  // TODO: Support doc
  @Composable
  fun Previewer(
    modifier: Modifier = Modifier,
    media: MediaItem? = null,
    onMediaSelected: (MediaItem?) -> Unit,
  ) {
    val context = LocalContext.current

    fun clearMedia() {
      onMediaSelected(null)
    }

    Row(modifier = modifier) {
      AnimatedContent(media != null) { show ->
        // TODO: show has conflict with media != null while render transform state.
        // Should enhance by rememberAsyncImagePainter to avoid loss animation.
        if (show) {
          Box(
            Modifier
              .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.medium)
              .clip(MaterialTheme.shapes.small)
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .padding(3.dp)
          ) {
            when (media) {
              is ImageMediaItem, is VideoMediaItem -> {
                AsyncImage(
                  model = ImageRequest.Builder(context)
                    .data(media?.uri)
                    .let {
                      if (media is VideoMediaItem) {
                        it.decoderFactory { result, options, loader ->
                          VideoFrameDecoder(
                            result.source,
                            options
                          )
                        }
                      } else {
                        it
                      }
                    }
                    .build(),
                  "预览",
                  contentScale = ContentScale.Crop,
                  placeholder = if (LocalInspectionMode.current) painterResource(R.drawable.avatar_sample) else null,
                  modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .size(120.dp)
                )
              }
              is DocumentMediaItem -> {
                Box(
                  Modifier
                    .clip(MaterialTheme.shapes.small)
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                  contentAlignment = Alignment.Center
                ) {
                  // TODO: mime to icon
                  Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_doc_icon),
                    contentDescription = "Document",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              else -> {
                // TODO: Support generic
              }
            }

            FilledTonalIconButton(
              onClick = { clearMedia() },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(30.dp),
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              )
            ) {
              Icon(Icons.Default.Clear, "删除", Modifier.size(18.dp))
            }
          }
        } else {
          Box {}
        }
      }
    }
  }

  interface MediaButtonScope {
    fun onClick()
  }

  @Composable
  fun TakeDocFileButton(
    mimeTypes: Array<DocumentMediaItem.DocType> = DocumentMediaItem.DocType.values(),
    onMediaSelected: (MediaItem?) -> Unit = {},
    content: @Composable MediaButtonScope.() -> Unit,
  ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pathManager = LocalPathManager.current

    val fileLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
      if (uri != null) {
        Log.i(TAG, "Selected file: $uri")
        coroutineScope.launch(Dispatchers.IO) {
          try {
            val response = pathManager.copyContentMediaToStorage(uri)
            val storageUri = response.storageUri
            val mime = response.mimeType
            val fileName = response.fileName

            Log.i(TAG, "Saved file to storage: $storageUri ($mime) ($fileName)")
            onMediaSelected(DocumentMediaItem(storageUri.toString(), mime, title = fileName))
          } catch (e: Exception) {
            Log.e(TAG, "Failed to save file to storage", e)
            Toast.makeText(context, "载入文档失败", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

    fun selectFile() {
      fileLauncher.launch(mimeTypes.map { it.mimeType }.toTypedArray())
    }

    content.invoke(object : MediaButtonScope {
      override fun onClick() {
        selectFile()
      }
    })
  }

  @Composable
  fun TakeGalleryButton(
    type: MediaType = MediaType.ImageAndVideo,
    onMediaSelected: (MediaItem?) -> Unit = {},
    content: @Composable MediaButtonScope.() -> Unit,
  ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pathManager = LocalPathManager.current

    // 从相册选择相关
    val galleryLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        Log.i(TAG, "已选择的媒体: $uri")
        coroutineScope.launch(Dispatchers.IO) {
          val (storageUri, mime) = pathManager.legacyCopyContentMediaToStorage(uri)
          Log.i(TAG, "已保存选择的媒体到存储: $storageUri ($mime)")

          val item = when {
            mime.startsWith("image/", true) -> ImageMediaItem(storageUri.toString())
            mime.startsWith("video/", true) -> {
              val snapshotMedia = VideoUtils.getFrame(context, storageUri)?.let {
                val filename = storageUri.lastPathSegment ?: "unknown-video.mp4"
                val imageUri = pathManager.saveBitmapToStorage(it, "SNAP-${filename}")
                Log.i(TAG, "已保存视频的快照到存储: $imageUri")

                ImageMediaItem(imageUri.toString())
              };
              VideoMediaItem(storageUri.toString(), snapshotMedia)
            }
            else -> GenericMediaItem(storageUri.toString(), mime)
          }
          onMediaSelected(item)
        }
      }
    }

    fun selectMediaGallery() {
      galleryLauncher.launch(
        PickVisualMediaRequest(
          when (type) {
            MediaType.Image -> ActivityResultContracts.PickVisualMedia.ImageOnly
            MediaType.Video -> ActivityResultContracts.PickVisualMedia.VideoOnly
            MediaType.ImageAndVideo -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
          }
        )
      )
    }

    // TODO: 支持添加选择的照片或者视频
    // https://developer.android.com/about/versions/14/changes/partial-photo-video-access?hl=zh-cn#media-reselection
    val permissionForGallery = rememberMultiplePermissionsStateSafe(PermissionUtils.visualMediaPermission) {
      if (it.values.any { true }) {
        selectMediaGallery()
      }
    }

    content.invoke(object : MediaButtonScope {
      override fun onClick() {
        if (permissionForGallery.permissions.any { it.status.isGranted }) {
          selectMediaGallery()
        } else {
          permissionForGallery.launchMultiplePermissionRequest()
        }
      }
    })
  }

  @Composable
  fun TakePhotoButton(
    // TODO: 改为 ImageMediaItem? -> Unit and 别的 in this file
    onMediaSelected: (MediaItem?) -> Unit = {},
    content: @Composable MediaButtonScope.() -> Unit,
  ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pathManager = LocalPathManager.current

    // 从相机拍照相关
    var tempCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture()
    ) { isSaved ->
      if (isSaved) {
        Log.i(TAG, "已保存拍摄的照片: $tempCameraPhotoUri")
        coroutineScope.launch {
          val uri = pathManager.legacyCopyContentMediaToStorage(tempCameraPhotoUri!!, ImageMediaItem::class)
          Log.i(TAG, "已保存拍摄的照片到存储: $uri")
          onMediaSelected(ImageMediaItem(uri.toString()))
        }
      } else {
        Log.i(TAG, "拍照取消")
      }
    }

    fun selectMediaCameraImage() {
      tempCameraPhotoUri = pathManager.createTempContentMediaUri(ImageMediaItem::class)
      Log.i(TAG, "将拍摄照片到: $tempCameraPhotoUri")
      cameraLauncher.launch(tempCameraPhotoUri!!)
    }

    val permissionForCameraImage = rememberPermissionStateSafe(android.Manifest.permission.CAMERA) {
      if (it) {
        selectMediaCameraImage()
      }
    }

    content.invoke(object : MediaButtonScope {
      override fun onClick() {
        if (permissionForCameraImage.status.isGranted) {
          selectMediaCameraImage()
        } else {
          permissionForCameraImage.launchPermissionRequest()
        }
      }
    })
  }

  @Composable
  fun TakeVideoButton(
    onMediaSelected: (MediaItem?) -> Unit = {},
    content: @Composable MediaButtonScope.() -> Unit,
  ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pathManager = LocalPathManager.current

    // 从相机录视频相关
    var tempCameraVideoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncherVideo = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CaptureVideo()
    ) { isSaved ->
      if (isSaved) {
        Log.i(TAG, "已保存录制的视频: $tempCameraVideoUri")
        coroutineScope.launch(Dispatchers.IO) {
          val uri = pathManager.legacyCopyContentMediaToStorage(tempCameraVideoUri!!, VideoMediaItem::class)

          val snapshotMedia = VideoUtils.getFrame(context, uri)?.let {
            val filename = uri.lastPathSegment ?: "unknown-video.mp4"
            val imageUri = pathManager.saveBitmapToStorage(it, "SNAP-${filename}")
            Log.i(TAG, "已保存视频的快照到存储: $imageUri")

            ImageMediaItem(imageUri.toString())
          };

          Log.i(TAG, "已保存录制的视频到存储: $uri")
          onMediaSelected(VideoMediaItem(uri.toString(), snapshotMedia))
        }
      } else {
        Log.i(TAG, "拍摄视频取消")
      }
    }

    fun selectMediaCameraVideo() {
      tempCameraVideoUri = pathManager.createTempContentMediaUri(VideoMediaItem::class)
      Log.i(TAG, "将录制视频到: $tempCameraVideoUri")
      cameraLauncherVideo.launch(tempCameraVideoUri!!)
    }

    val permissionForCameraVideo = rememberPermissionStateSafe(android.Manifest.permission.CAMERA) {
      if (it) {
        selectMediaCameraVideo()
      }
    }

    content.invoke(object : MediaButtonScope {
      override fun onClick() {
        if (permissionForCameraVideo.status.isGranted) {
          selectMediaCameraVideo()
        } else {
          permissionForCameraVideo.launchPermissionRequest()
        }
      }
    })
  }

  // TODO: 消除重复代码
  // TODO: 支持选择文件
  @Composable
  fun BottomModal(
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    onMediaSelected: (MediaItem?) -> Unit,
    type: MediaType = MediaType.ImageAndVideo,
  ) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val pathManager = LocalPathManager.current
    val coroutineScope = rememberCoroutineScope()

    // 从相机拍照相关
    var tempCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture()
    ) { isSaved ->
      if (isSaved) {
        Log.i(TAG, "已保存拍摄的照片: $tempCameraPhotoUri")
        coroutineScope.launch {
          val uri = pathManager.legacyCopyContentMediaToStorage(tempCameraPhotoUri!!, ImageMediaItem::class)
          Log.i(TAG, "已保存拍摄的照片到存储: $uri")
          onMediaSelected(ImageMediaItem(uri.toString()))
          sheetState.hide()
          onDismiss()
        }
      } else {
        Log.i(TAG, "拍照取消")
      }
    }

    fun selectMediaCameraImage() {
      tempCameraPhotoUri = pathManager.createTempContentMediaUri(ImageMediaItem::class)
      Log.i(TAG, "将拍摄照片到: $tempCameraPhotoUri")
      cameraLauncher.launch(tempCameraPhotoUri!!)
    }

    val permissionForCameraImage = rememberPermissionState(android.Manifest.permission.CAMERA) {
      if (it) {
        selectMediaCameraImage()
      }
    }

    // 从相机录视频相关
    var tempCameraVideoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncherVideo = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CaptureVideo()
    ) { isSaved ->
      if (isSaved) {
        Log.i(TAG, "已保存录制的视频: $tempCameraVideoUri")
        coroutineScope.launch(Dispatchers.IO) {
          val uri = pathManager.legacyCopyContentMediaToStorage(tempCameraVideoUri!!, VideoMediaItem::class)

          val snapshotMedia = VideoUtils.getFrame(context, uri)?.let {
            val filename = uri.lastPathSegment ?: "unknown-video.mp4"
            val imageUri = pathManager.saveBitmapToStorage(it, "SNAP-${filename}")
            Log.i(TAG, "已保存视频的快照到存储: $imageUri")

            ImageMediaItem(imageUri.toString())
          };

          Log.i(TAG, "已保存录制的视频到存储: $uri")
          onMediaSelected(VideoMediaItem(uri.toString(), snapshotMedia))

          withContext(Dispatchers.Main) {
            sheetState.hide()
            onDismiss()
          }
        }
      } else {
        Log.i(TAG, "拍摄视频取消")
      }
    }

    fun selectMediaCameraVideo() {
      tempCameraVideoUri = pathManager.createTempContentMediaUri(VideoMediaItem::class)
      Log.i(TAG, "将录制视频到: $tempCameraVideoUri")
      cameraLauncherVideo.launch(tempCameraVideoUri!!)
    }

    val permissionForCameraVideo = rememberPermissionState(android.Manifest.permission.CAMERA) {
      if (it) {
        selectMediaCameraVideo()
      }
    }

    // 从相册选择相关
    val galleryLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        Log.i(TAG, "已选择的媒体: $uri")
        coroutineScope.launch(Dispatchers.IO) {
          val (storageUri, mime) = pathManager.legacyCopyContentMediaToStorage(uri)
          Log.i(TAG, "已保存选择的媒体到存储: $storageUri ($mime)")

          val item = when {
            mime.startsWith("image/", true) -> ImageMediaItem(storageUri.toString())
            mime.startsWith("video/", true) -> {
              val snapshotMedia = VideoUtils.getFrame(context, storageUri)?.let {
                val filename = storageUri.lastPathSegment ?: "unknown-video.mp4"
                val imageUri = pathManager.saveBitmapToStorage(it, "SNAP-${filename}")
                Log.i(TAG, "已保存视频的快照到存储: $imageUri")

                ImageMediaItem(imageUri.toString())
              };
              VideoMediaItem(storageUri.toString(), snapshotMedia)
            }
            else -> GenericMediaItem(storageUri.toString(), mime)
          }
          onMediaSelected(item)

          withContext(Dispatchers.Main) {
            sheetState.hide()
            onDismiss()
          }
        }
      }
    }

    fun selectMediaGallery() {
      galleryLauncher.launch(
        PickVisualMediaRequest(
          when (type) {
            MediaType.Image -> ActivityResultContracts.PickVisualMedia.ImageOnly
            MediaType.Video -> ActivityResultContracts.PickVisualMedia.VideoOnly
            MediaType.ImageAndVideo -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
          }
        )
      )
    }

    // TODO: 支持添加选择的照片或者视频
    // https://developer.android.com/about/versions/14/changes/partial-photo-video-access?hl=zh-cn#media-reselection
    val permissionForGallery = rememberMultiplePermissionsStateSafe(PermissionUtils.visualMediaPermission) {
      if (it.values.any { true }) {
        selectMediaGallery()
      }
    }

    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = onDismiss,
      modifier = modifier,
//      windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
    ) {
      Box(
        Modifier
          .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
          .padding(horizontal = 8.dp)
          .padding(bottom = 26.dp)
      ) {
        Column {
          val hasVideo = listOf(MediaType.Video, MediaType.ImageAndVideo).contains(type)

          if (hasVideo) {
            MediaSelectorItem(stringResource(R.string.label_take_video_full), FontAwesomeIcons.Solid.Video) {
              if (permissionForCameraVideo.status.isGranted) {
                selectMediaCameraVideo()
              } else {
                permissionForCameraVideo.launchPermissionRequest()
              }
            }
            Spacer(Modifier.height(8.dp))
          }

          MediaSelectorItem(stringResource(R.string.label_take_photo_full), ImageVector.vectorResource(R.drawable.ic_camera_icon)) {
            if (permissionForCameraImage.status.isGranted) {
              selectMediaCameraImage()
            } else {
              permissionForCameraImage.launchPermissionRequest()
            }
          }
          Spacer(Modifier.height(8.dp))
          MediaSelectorItem(stringResource(R.string.label_gallery_full), FontAwesomeIcons.Solid.Image) {
            if (permissionForGallery.permissions.any { it.status.isGranted }) {
              selectMediaGallery()
            } else {
              permissionForGallery.launchMultiplePermissionRequest()
            }
          }
        }
      }
    }
  }

  @Composable
  private fun MediaSelectorItem(label: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
      Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, label, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(12.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

/**
 * 该 Demo 运行一个选择多媒体的 Demo App
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MediaSelectorPreview() {
  var show by remember { mutableStateOf(false) }
  var mediaItem by remember { mutableStateOf<MediaItem?>(null) }

  val pathManager = PathManager(LocalContext.current)

  CompositionLocalProvider(LocalPathManager provides pathManager) {
    Box(Modifier.fillMaxSize()) {
      Column(modifier = Modifier.align(Alignment.Center)) {
        Button(
          modifier = Modifier,
          onClick = {
            show = !show
          }
        ) {
          Text("选择多媒体")
        }

        MediaSelector.TakeDocFileButton(
          onMediaSelected = {
            println("TakeFileButton onMediaSelected: $it")
            mediaItem = it
          }
        ) {
          Button(
            onClick = {
              onClick()
            }
          ) {
            Text("选择文件")
          }
        }
      }

      if (show) {
        MediaSelector.BottomModal(
          onDismiss = {
            println("onDismiss BottomModal")
            show = false
          },
          onMediaSelected = { mediaItem = it },
          modifier = Modifier
        )
      }

      MediaSelector.Previewer(
        media = mediaItem,
        onMediaSelected = { mediaItem = it },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(20.dp),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MediaPreviewerImagePreview() {
  val mediaItem = ImageMediaItem("https://picsum.photos/200/300")
  MediaSelector.Previewer(media = mediaItem) {}
}