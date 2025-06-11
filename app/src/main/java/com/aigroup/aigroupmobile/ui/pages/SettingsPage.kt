@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class
)

package com.aigroup.aigroupmobile.ui.pages

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.Screen
import com.aigroup.aigroupmobile.ui.pages.settings.components.SettingItems
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.models.RemoteTokenConfig
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.ui.components.theme.GradientLayer2
import com.aigroup.aigroupmobile.ui.components.theme.StylizedBackgroundLayer
import com.aigroup.aigroupmobile.ui.pages.settings.SettingPageAppBar
import com.aigroup.aigroupmobile.ui.pages.settings.UserProfileContentData
import com.aigroup.aigroupmobile.ui.pages.settings.UserProfileInfo
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.fadingEdge
import com.aigroup.aigroupmobile.ui.utils.withoutBottom
import com.aigroup.aigroupmobile.viewmodels.SettingsViewModel
import com.aigroup.aigroupmobile.viewmodels.UserProfileViewModel
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.jvziyaoyao.scale.image.previewer.ImagePreviewer
import com.jvziyaoyao.scale.zoomable.pager.PagerGestureScope
import com.jvziyaoyao.scale.zoomable.previewer.rememberPreviewerState
import compose.icons.CssGgIcons
import compose.icons.FontAwesomeIcons
import compose.icons.cssggicons.SoftwareDownload
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Share
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable()
fun SettingPageBackground() {
//  val bgColor1: Color = Color(0x64DBCEEF)
//  val bgColor2: Color = Color(0x6499AFED)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AppCustomTheme.colorScheme.groupedBackground)
//      .background(
//        brush = Brush.linearGradient(
//          colors = listOf(
//            bgColor1,
//            bgColor2,
//          )
//        )
//      )
  ) {
    StylizedBackgroundLayer(modifier = Modifier.alpha(0.7f), variant = 2)
  }
}

@Composable
private fun SettingPageProfileContent(
  onOpenPage: (Screen) -> Unit,
  viewModel: UserProfileViewModel = hiltViewModel(),
  modifier: Modifier = Modifier,
  mediaItems: List<MediaItem> = emptyList(),
  onPreview: (MediaItem) -> Unit = {},
  content: @Composable () -> Unit
) {
  val userProfile by viewModel.profile.observeAsState(UserProfile())
  val conversationCount by viewModel.conversationsCount.observeAsState(0)
  val assistantsCount by viewModel.assistantsCount.observeAsState(0)
  val commonlyModelCode by viewModel.commonlyModelCode.observeAsState(emptyList())
  val context = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    SettingPageBackground()

    LazyColumn(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 86.dp)
    ) {
      // main content
      item {
        UserProfileContentData(
          Modifier,
          userProfile,
          userProfileInfo = listOf(
            UserProfileInfo(
              stringResource(R.string.label_settings_sessions_count),
              ImageVector.vectorResource(R.drawable.ic_bubble_caption_icon),
              "${conversationCount}个",
              iconModifier = Modifier.size(20.dp)
            ),
            UserProfileInfo(
              stringResource(R.string.label_settings_assistants),
              ImageVector.vectorResource(R.drawable.ic_custom_bot_icon),
              "${assistantsCount}个",
              iconModifier = Modifier.size(20.dp)
            ),
            UserProfileInfo(
              stringResource(R.string.label_settings_commonly_llm_model),
              if (commonlyModelCode.isEmpty()) {
                ImageVector.vectorResource(R.drawable.ic_custom_bot_icon)
              } else {
                val iconId = ModelCode(commonlyModelCode.first(), ChatServiceProvider.OFFICIAL).iconId
                ImageVector.vectorResource(iconId)
              },
              if (commonlyModelCode.isEmpty()) {
                stringResource(R.string.label_settings_no_commonly_llm_model)
              } else {
                commonlyModelCode.first()
              },
              iconModifier = Modifier.size(15.dp)
            ),
          ),
          onOpenPage = onOpenPage
        )
      }

      item {
        Surface(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          shape = MaterialTheme.shapes.medium
        ) {
          Column(
            Modifier.padding(vertical = 16.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
              Icon(
                ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy), "", Modifier.size(18.dp)
              )
              Spacer(Modifier.width(5.dp))
              Text(stringResource(R.string.label_settings_my_media_content), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier) {
              val leftRightFade = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.05f to Color.White,
                0.95f to Color.White,
                1f to Color.Transparent
              )
              LazyRow(
                Modifier
                  .fillMaxWidth()
                  .height(100.dp)
                  .fadingEdge(leftRightFade),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(mediaItems) {
                  Box(
                    Modifier
                      .clip(MaterialTheme.shapes.small)
                      .clickable {
                        onPreview(it)
                      }) {
                    when (it) {
                      is ImageMediaItem -> {
                        AsyncImage(
                          it.url,
                          "图片",
                          contentScale = ContentScale.Crop,
                          modifier = Modifier
                            .width(100.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                      }

                      is VideoMediaItem -> {
                        AsyncImage(
                          ImageRequest.Builder(context)
                            .data(it.uri)
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
                            .width(100.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                      }

                      else -> {}
                    }
                  }
                }
              }
              if (mediaItems.isEmpty()) {
                Box(
                  Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                ) {
                  Column(
                    Modifier
                      .fillMaxWidth()
                      .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Icon(
                      ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy),
                      "",
                      tint = AppCustomTheme.colorScheme.secondaryLabel,
                      modifier = Modifier.size(60.dp)
                    )
                    Text(
                      stringResource(R.string.label_settings_media_content_empty),
                      style = MaterialTheme.typography.labelMedium,
                      color = AppCustomTheme.colorScheme.secondaryLabel,
                    )
                  }
                }
              }
            }
          }
        }
      }

      item {
        content()
      }
    }
  }
}

@Composable
private fun SettingPageMainContent(
  onOpenPage: (Screen) -> Unit,
  viewModel: SettingsViewModel = hiltViewModel()
) {
  // TODO: 使用 main activity 初始化读取到的？或者延长 SettingsViewModel
  val appPreferences by viewModel.preferences.collectAsStateWithLifecycle(AppPreferences.getDefaultInstance())

  val cacheStatus by viewModel.cacheStatus.collectAsStateWithLifecycle()
  val localDataStatus by viewModel.localDataStatus.collectAsStateWithLifecycle()
  val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
//    println("initialize setting page status...")
    viewModel.updateCacheSize()
    viewModel.updateBackupStatus()
  }

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  fun backupUserData() {
    coroutineScope.launch {
      val target = viewModel.backupUserData()
      withContext(Dispatchers.Main) {
        println("write backup status: $target")

        // share this zip
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_STREAM, target)
          type = "application/zip"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
      }
    }
  }

  val pathManager = LocalPathManager.current
  val appName = LocalContext.current.getString(R.string.app_name)

  fun shareApp() {
    val apkPath = context.applicationInfo.sourceDir

    coroutineScope.launch(Dispatchers.IO) {
      val uri = pathManager.copyFileToTemp(File(apkPath))

      withContext(Dispatchers.Main) {
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          type = "application/vnd.android.package-archive"

          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_TITLE, context.getString(R.string.label_settings_share, appName))

          flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
      }
    }
  }

  SettingItems(
    appPreferences = appPreferences,
    update = viewModel::updatePreferences,
    onOpenPage = onOpenPage,
    onShareApp = { shareApp() },
    // cache
    cacheStatus = cacheStatus,
    onClearCache = viewModel::clearCache,
    // clear data
    localDataStatus = localDataStatus,
    onClearUserData = viewModel::clearUserLocalData,
    // backup data
    backupStatus = backupStatus, // 丧失 dataStore 响应式，但是支持网络备份状态的获取
    onUserDataBackup = {
      backupUserData()
    },
  )
}

@Composable()
fun SettingsPage(
  onOpenPage: (Screen) -> Unit,
  onBack: () -> Unit,
  viewModel: UserProfileViewModel = hiltViewModel(),
  settingsViewModel: SettingsViewModel = hiltViewModel()
) {
  val userProfile by viewModel.profile.observeAsState(UserProfile())
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val mediaItems: List<Pair<MediaItem, String?>> by viewModel.mediaItems.observeAsState(emptyList())

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val previewerState = rememberPreviewerState(pageCount = {
    mediaItems.count()
  })

  var showAddRemoteTokenConfirm by remember { mutableStateOf(false) }

  // TODO: remote this var by support show a dialog using suspend (see flutter dialog api)
  var remoteTokenConfig by remember { mutableStateOf<RemoteTokenConfig?>(null) }

  fun showConfirmForRemoteConfig(config: RemoteTokenConfig) {
    remoteTokenConfig = config
    showAddRemoteTokenConfirm = true
  }

  fun confirmSetupRemoteConfig() {
    remoteTokenConfig?.let {
      settingsViewModel.updateAllTokenPreferences(it.tokens)
      settingsViewModel.updateServiceTokenPreferences(it.serviceTokens)
      settingsViewModel.updateTokenConfigs(it.tokenConfigs)
    }
    Toast.makeText(context, context.getString(R.string.toast_add_remove_config_success), Toast.LENGTH_SHORT).show()
  }

  Scaffold(
    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SettingPageAppBar(
        userProfile, scrollBehavior, onBack = onBack,
        onUpdateToken = {
          coroutineScope.launch {
            try {
              val remoteConfig = settingsViewModel.retrieveConfigByAppToken(it)
              showConfirmForRemoteConfig(remoteConfig)
            } catch (e: Exception) {
              Toast.makeText(
                context,
                context.getString(R.string.toast_add_remove_config_fail, e.message), Toast.LENGTH_SHORT
              ).show()
            }
          }
        }
      )
    }
  ) { innerPadding ->
    SettingPageProfileContent(
      onOpenPage = onOpenPage,
      modifier = Modifier.padding(innerPadding.withoutBottom()),
      mediaItems = mediaItems.map { it.first },
      onPreview = { media ->
        coroutineScope.launch {
          // TODO: 考虑 url 可能一样的情况？
          val index = mediaItems.indexOfFirst {
            it.first.url == media.url
          }
          previewerState.open(index)
        }
      }
    ) {
      SettingPageMainContent(
        onOpenPage = onOpenPage
      )
    }
  }

  if (showAddRemoteTokenConfirm) {
    // TODO: show loading status, 加载配置需要时间，显示 loading alert and disallow dismiss
    AlertDialog(
      icon = {
        Icon(
          ImageVector.vectorResource(R.drawable.ic_secret_files_icon),
          contentDescription = null,
          Modifier.size(25.dp)
        )
      },
      title = {
        Text(text = stringResource(R.string.label_settings_add_remote_config))
      },
      text = {
        Text(text = stringResource(R.string.label_settings_confirm_add_remote_config))
      },
      onDismissRequest = {
        showAddRemoteTokenConfirm = false
      },
      confirmButton = {
        Button(
          onClick = {
            showAddRemoteTokenConfirm = false
            confirmSetupRemoteConfig()
          },
          shape = MaterialTheme.shapes.medium,
        ) {
          Text(
            text = stringResource(R.string.label_confirm),
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showAddRemoteTokenConfirm = false },
          shape = MaterialTheme.shapes.medium
        ) {
          Text(stringResource(R.string.label_cancel))
        }
      }
    )
  }

  // TODO: (REFACTOR) ImagePreview 部分和 [ChatDetailPage] 代码重复

  val pathManager = LocalPathManager.current

  fun downloadMedia(media: MediaItem) {
    coroutineScope.launch {
      val success = when (media) {
        is ImageMediaItem -> {
          pathManager.saveImageToGallery(media.uri) != null
        }

        is VideoMediaItem -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pathManager.saveVideoToGallery(media.uri) != null
          } else {
            false
          }
        }

        else -> false
      }
      if (success) {
        Toast.makeText(context, context.getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, context.getString(R.string.toast_save_fail), Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun shareMedia(media: MediaItem) {
    coroutineScope.launch {
      when (media) {
        is ImageMediaItem -> {
          // TODO: normalize filename
          val uri = pathManager.copyFileToTemp(media.uri.path!!, "share.jpg")
          val shareIntent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.putExtra(Intent.EXTRA_STREAM, uri)
            this.setDataAndType(uri, "image/jpeg") // TODO: correct mime
            this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
          }
          context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.label_share_to)))
        }

        is VideoMediaItem -> {
          // TODO: normalize filename
          val uri = pathManager.copyFileToTemp(media.uri.path!!, "share.mp4")
          val shareIntent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.putExtra(Intent.EXTRA_STREAM, uri)
            this.setDataAndType(uri, "video/mp4") // TODO: correct mime
            this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
          }
          context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.label_share_to)))
        }

        else -> {
          // TODO: handle other types
        }
      }
    }
  }

  val idxToVideoPlayer: MutableMap<Int, ExoPlayer> = remember {
    mutableMapOf()
  }

  LaunchedEffect(previewerState.currentPage) {
    if (mediaItems.isEmpty()) {
      return@LaunchedEffect
    }

    // 当预览 pager 移动 page 时，应该暂停所有视频
    idxToVideoPlayer.forEach { (_, player) ->
      player.pause()
    }
  }

  ImagePreviewer(
    modifier = Modifier.fillMaxSize(),
    state = previewerState,
    detectGesture = PagerGestureScope(onTap = {
      coroutineScope.launch {
        val item = mediaItems[previewerState.currentPage]
        if (item.first is VideoMediaItem) {
          // PASS
        } else {
          // 关闭预览组件
          previewerState.close()
        }
      }
    }),
    imageLoader = { index ->
      val item = mediaItems[index]
      val painter = if (item.first is ImageMediaItem) {
        rememberAsyncImagePainter(
          item.first.url,
        )
      } else {
        rememberAsyncImagePainter(
          ImageRequest.Builder(context)
            .data(item.first.url)
            .decoderFactory { result, options, loader ->
              VideoFrameDecoder(
                result.source,
                options
              )
            }
            .build()
        )
      }

      Pair(painter, painter.intrinsicSize)
    },
    pageDecoration = { idx, innerPage ->
      val mediaItem = mediaItems[idx]
      var mounted = false

      // 单独设置每一页的背景颜色
      Box {
        // 通过调用页面获取imageLoader的状态
        mounted = innerPage()

//        val currentShow = previewerState.currentPage == idx
        if (mediaItem.first is VideoMediaItem) {
          VideoPlayer(
            mediaItems = listOf(
              VideoPlayerMediaItem.StorageMediaItem(
                storageUri = mediaItem.first.uri,
              ),
            ),
            handleLifecycle = false,
            autoPlay = false,
            usePlayerController = true,
            controllerConfig = VideoPlayerControllerConfig(
              showSpeedAndPitchOverlay = false,
              showSubtitleButton = false,
              showCurrentTimeAndTotalTime = true,
              showBufferingProgress = false,
              showForwardIncrementButton = true,
              showBackwardIncrementButton = true,
              showBackTrackButton = false,
              showNextTrackButton = false,
              showRepeatModeButton = false,
              controllerShowTimeMilliSeconds = 5_000,
              controllerAutoShow = true,
              showFullScreenButton = false
            ),
            enablePip = false,
            handleAudioFocus = true,
            volume = 0.5f,  // volume 0.0f to 1.0f
            modifier = Modifier
              .fillMaxSize()
              .align(Alignment.Center)
              .safeDrawingPadding(),
            playerInstance = {
              idxToVideoPlayer[idx] = this
            }
          )
        }

        // 设置前景图层
        TopAppBar(
          title = { },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            actionIconContentColor = Color.White,
          ),
          actions = {
            if (mediaItem.first !is VideoMediaItem || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              FilledIconButton(
                onClick = { downloadMedia(mediaItem.first) },
                shape = MaterialTheme.shapes.large,
                colors = IconButtonDefaults.iconButtonColors(
                  containerColor = MaterialColors.Gray[900],
                  contentColor = Color.White
                )
              ) {
                Icon(CssGgIcons.SoftwareDownload, "", Modifier.size(18.dp))
              }
            }

            FilledIconButton(
              onClick = { shareMedia(mediaItem.first) },
              shape = MaterialTheme.shapes.large,
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialColors.Gray[900],
                contentColor = Color.White
              )
            ) {
              Icon(FontAwesomeIcons.Solid.Share, "", Modifier.size(15.dp))
            }
          }
        )

        if (mediaItem.second != null) {
          BottomAppBar(
            Modifier.align(Alignment.BottomCenter),
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
          ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
              Text(mediaItem.second!!, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
      // 这里需要返回页面的挂载情况
      mounted
    }
  )

  BackHandler(enabled = previewerState.visible) {
    coroutineScope.launch {
      previewerState.close()
    }
  }
}

