@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalLayoutApi::class
)

package com.aigroup.aigroupmobile.ui.pages

import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.GuideQuestion
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences.ChatViewMode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.data.models.data
import com.aigroup.aigroupmobile.data.models.loading
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.data.models.unifiedTitle
import com.aigroup.aigroupmobile.data.utils.hasSetToken
import com.aigroup.aigroupmobile.dataStore
import com.aigroup.aigroupmobile.services.chat.plugins.BuiltInPlugins
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScopeRunner
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.ChatAppBar
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.ChatBottomBar
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.ChatBottomBarExtra
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.ChatIntroPage
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessageItem
import com.aigroup.aigroupmobile.ui.components.theme.StylizedBackgroundLayer
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessageContentScope
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.rememberChatBottomBarUIState
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.services.rememberSecretSpeechRecognitionState
import com.aigroup.aigroupmobile.utils.system.OpenExternal
import com.aigroup.aigroupmobile.viewmodels.ChatConversationViewModel
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel
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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

private const val TAG = "ChatDetailPage"

@OptIn(InternalCoroutinesApi::class)
@Composable
fun ChatDetailPage(
  onOpenDrawer: () -> Unit,
  onOpenSetting: () -> Unit,
  onNavigateDetail: (id: String) -> Unit,
  onOpenUserProfile: () -> Unit = {},
  onNavigateToAssistantStore: () -> Unit = {},

  chatViewModel: ChatViewModel = hiltViewModel(),
  userProfileViewModel: UserProfileViewModel = hiltViewModel(),
  sessionsViewModel: ChatConversationViewModel = hiltViewModel(),
  settingViewModel: SettingsViewModel = hiltViewModel(),

  splitMode: Boolean = false,
) {
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  val userProfile by userProfileViewModel.profile.observeAsState(UserProfile());

  val chatSession by chatViewModel.session.observeAsState()
  val primaryBotSender = chatSession?.primaryBotSender?.botSender

  val allMessages by chatViewModel.allMessages.map { LoadingStatus.Success(it) }
    .observeAsState(LoadingStatus.Loading)
  val bottomBarState by chatViewModel.bottomBarState.collectAsStateWithLifecycle()
  val loadingId by chatViewModel.loadingId.collectAsStateWithLifecycle(initialValue = null)

  // TODO: mark all collectAsState and observeAsStates 's null state or initial state will make performance problem?
  val chatViewMode by chatViewModel.chatViewMode.collectAsStateWithLifecycle(ChatViewMode.UNRECOGNIZED)
  // TODO: 仅在 bubble 模式下才计算，不要和 data 同时计算
  val bubbleMessages by chatViewModel.bubbleMessages.observeAsState(listOf()) // TODO: observe only when chatViewMode is BUBBLE

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val scrollViewState = rememberLazyListState()
  val imeVisible = WindowInsets.isImeVisible
  LaunchedEffect(imeVisible) {
    if (imeVisible) {
      scrollViewState.animateScrollToItem(0)
    }
  }

  val data = allMessages.data ?: emptyList()
  LaunchedEffect(data.count()) {
    if (data.isNotEmpty())
      scrollViewState.animateScrollToItem(0)
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val keyboardShow = WindowInsets.isImeVisible

  val nestedScrollConnection = remember(keyboardController, keyboardShow) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (keyboardShow) {
          keyboardController?.hide()
        }
        return Offset.Zero
      }
    }
  }

  val context = LocalContext.current
  val bottomBarUIState = rememberChatBottomBarUIState()
  val bottomBarSpeechState = rememberSecretSpeechRecognitionState()
  val pinModels by settingViewModel.favoriteModels.map { LoadingStatus.Success(it) }.observeAsState(
    initial = LoadingStatus.Loading
  )
  var showTokenDialog by remember { mutableStateOf(false) }

  fun onSend(content: String? = null) {
    // TODO: get not set token exception from BaseChatCoordinator
    val serviceProvider = primaryBotSender?.langBot?.model?.serviceProvider
    if (serviceProvider == null) {
      Log.w(TAG, "serviceProvider is null")
    }

    coroutineScope.launch {
      // TODO: manage in viewModel
      val hasToken = context.dataStore.data.map {
        it.token.hasSetToken(serviceProvider!!)
      }.first()
      if (!hasToken) {
        showTokenDialog = true
      } else {
        chatViewModel.sendContent(explicitContent = content)
      }
    }
  }

  fun onSelectGuide(guide: GuideQuestion) {
    chatViewModel.updateInputText(guide.question)
    when (guide.type) {
      GuideQuestion.GuideQuestionType.TEXT -> {
        chatViewModel.sendContent()
      }

      GuideQuestion.GuideQuestionType.PREFERENCE -> {
        chatViewModel.respondPreference(guide)
      }
    }
  }

  val hapic = LocalHapticFeedback.current
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

  val hazeState = remember { HazeState() }

  val mediaPreviewList by chatViewModel.mediaItems.observeAsState(initial = emptyList())
  val previewerState = rememberPreviewerState(pageCount = {
    mediaPreviewList.count()
  })

  Scaffold(
    modifier = Modifier
      .nestedScroll(scrollBehavior.nestedScrollConnection)
      .imePadding(),
    topBar = {
      ChatAppBar(
        // TODO: 考虑有的情况会忽略一些空 message 比如 bubble present 里面
        createNewSessionEnable = allMessages.data?.isNotEmpty() == true,
        onCreateNewSession = {
          coroutineScope.launch {
            val new = sessionsViewModel.createEmptySessionIfNotExists()
            onNavigateDetail(new.id.toHexString())
          }
        },

        hazeState = hazeState,
        title = chatSession?.unifiedTitle ?: stringResource(R.string.label_loading),
        onOpenDrawer = onOpenDrawer,
        onOpenUserProfile = onOpenUserProfile,
        scrollBehavior = scrollBehavior,
        onDeleteChat = {
          chatSession?.also {
            coroutineScope.launch {
              val next = sessionsViewModel.deleteChatSession(it)
              onNavigateDetail(next.id.toHexString())
            }
          }
        },
        onUpdateTitle = { title ->
          chatSession?.also {
            sessionsViewModel.updateChatSessionTitle(it, title)
          }
        },
        onSummarySession = {
          chatViewModel.summarySession(reset = true)
        },
        onShowSettings = {
          onOpenSetting()
        },
        bot = chatSession?.primaryBotSender?.botSender,
        userProfile = userProfile,

        showToggleDrawerButton = !splitMode,
      )
    },
    bottomBar = {
      // TODO: move out of ChatDetailPage to nearest route root
      ChatBottomBar(
        bottomBarState = bottomBarState,
        bottomBarUIState = bottomBarUIState,
        speechRecognitionState = bottomBarSpeechState,
        onSend = { onSend(it) },
        hazeState = hazeState,
        onStateChanged = { chatViewModel.updateBottomBarState(it) },
        currentBot = chatSession?.primaryBot,
        contextIncludeCount = chatSession?.historyInclude,
        pluginList = chatSession?.plugins ?: emptyList(),
        onContextIncludeCountChange = {
          chatViewModel.updateChatSessionHistoryInclude(chatSession!!, it)
        },
        enablePlugin = {
          chatViewModel.enablePlugin(chatSession!!, it)
        },
        disablePlugin = {
          chatViewModel.disablePlugin(chatSession!!, it)
        },
        models = pinModels,
        onUpdateModels = {
          // 原先做更新模型的地方，会调用 ModelRepository 的 updateModelsIfNeeded 方法拉取各提供商的模型列表
          // 现在改为仅在 ModelPopup 显示用户收藏的模型，但是留该方法作为 placeholder
//          settingViewModel.updateModelsIfNeeded()
        },
        onSelectModel = {
          sessionsViewModel.updateChatSessionDefaultModel(chatSession!!, it)
          val assistant = primaryBotSender?.assistant

          // TODO: apply specific icon
          if (assistant != null) {
            Toast.makeText(
              context,
              context.getString(R.string.label_changed_chat_model_for_assistant, it.code, primaryBotSender.username),
              Toast.LENGTH_SHORT
            ).show()

          } else {
            Toast.makeText(context, context.getString(R.string.label_changed_chat_model, it.code), Toast.LENGTH_SHORT)
              .show()
          }

          hapic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onResetProperties = {
          chatViewModel.resetChatProperties()
        },
        onUpdateProperties = {
          chatViewModel.updateChatProperties(it)
        },
      )
    },
    containerColor = AppCustomTheme.colorScheme.groupedBackground
  ) { innerPadding ->
    Box(
      Modifier
        .fillMaxSize()
        .nestedScroll(nestedScrollConnection)
        .haze(
          state = hazeState,
          style = HazeDefaults.style(
            backgroundColor = AppCustomTheme.colorScheme.groupedBackground,
            blurRadius = 50.dp,
            noiseFactor = 10f
          )
        ),
    ) {
      val showEmptyState = !allMessages.loading && allMessages.data?.isEmpty() == true && primaryBotSender != null

      val backgroundAlpha by animateFloatAsState(if (showEmptyState) 1.0f else 0.5f)
      StylizedBackgroundLayer(modifier = Modifier.alpha(backgroundAlpha))

      when (chatViewMode) {
        ChatViewMode.BUBBLE -> {
          SelectionContainer {
            LazyColumn(
              state = scrollViewState,
              contentPadding = PaddingValues(
                top = 10.dp,
                bottom = innerPadding.calculateBottomPadding() + 50.dp
              ),
              modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .padding(top = innerPadding.calculateTopPadding()),
              reverseLayout = true
            ) {
              items(
                bubbleMessages,
                key = { it.id }
              ) { presentUnit ->
                val message = presentUnit.message

                val scope = object : ChatMessageContentScope {
                  override val chatViewModel: ChatViewModel
                    get() = chatViewModel
                  override val coroutineScope: CoroutineScope
                    get() = coroutineScope
                  override val isLoading: Boolean
                    get() = loadingId == message.id.toHexString()

                  override suspend fun previewMedia(media: MediaItem) {
                    if (media is DocumentMediaItem) {
                      return
                    }

                    // TODO: 考虑 url 可能一样的情况？
                    mediaPreviewList.indexOfFirst {
                      it.mediaItem.url == media.url
                    }.let {
                      previewerState.open(it)
                    }
                  }

                  override fun baseClickBehavior() {
                    focusManager.clearFocus()
                  }

                  override fun withPluginExecutor(run: ChatPluginRunScopeRunner) {
                    val pluginId = chatSession?.plugins?.firstOrNull {
                      it == message.pluginId
                    }
                    val plugin = BuiltInPlugins.plugins.firstOrNull {
                      it.name.lowercase() == pluginId?.lowercase()
                    }?.create()

                    // TODO: 未来加入 kotlin multiplatform 的 llm 调度，而不是放在 viewModel 里，
                    // 这样关于 plugin 的 executor 的调用可以放在 llm 调度器里而不是在 view 里调用 viewModel
                    // viewModel 可以类似 viewModelScope 有自己的生命周期 scope
                    // TODO: using scope instance in chat coordinator
                    chatViewModel.viewModelScope.launch {
                      chatViewModel.chatPluginExecutor.runScope(
                        presentUnit,
                        plugin,
                        run
                      )
                    }
                  }
                }

                // 执行副作用
                LaunchedEffect(message.pluginExtra) {
                  if (message.sender?.specific is MessageSenderBot && !message.pluginId.isNullOrEmpty()) {
                    chatViewModel.chatPluginExecutor.coordinateEffectNextRun(presentUnit, false)
                  }
                }

                presentUnit.Content(
                  // TODO: 验证该动画
                  modifier = Modifier.animateItem(placementSpec = null),
                  scope = scope
                )
              }
            }
          }
        }

        ChatViewMode.DOCUMENT -> {
          LazyColumn(
            state = scrollViewState,
            contentPadding = PaddingValues(
              top = 10.dp,
              bottom = innerPadding.calculateBottomPadding() + 50.dp
            ),
            modifier = Modifier
              .consumeWindowInsets(innerPadding)
              .padding(top = innerPadding.calculateTopPadding()),
            reverseLayout = true
          ) {
            items(data, key = { it.id.toHexString() }) {
              ChatMessageItem(
                message = it,
                loading = loadingId == it.id.toHexString(),
                onRetry = {
                  chatViewModel.retryMessage(it)
                },
                // TODO: 简化 session 获取逻辑（下面注释的写法）it.session 没有 observe
//                voiceCode = it.session.firstOrNull()?.voiceCode ?: AppPreferencesDefaults.defaultVoiceCode,
                voiceCode = chatSession?.voiceCode ?: AppPreferencesDefaults.defaultVoiceCode,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp)
                  .animateItem(
                    placementSpec = null
                  )
              )
            }
          }
        }

        else -> {}
      }

      Box(
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        AnimatedVisibility(showEmptyState, Modifier.fillMaxSize()) {
          val lastSession by sessionsViewModel.lastNormalSession.observeAsState()

          ChatIntroPage(
            primaryBotSender!!,
            userProfile,
            isRecording = bottomBarSpeechState.isRecognizing,
            onSelectGuideQuestion = {
              onSelectGuide(it)
            },
            onOpenSelectModel = {
              // see comment at ChatBottomBar.onUpdateModels
//                settingViewModel.updateModelsIfNeeded()
              bottomBarUIState.openModelsPopup()
            },
            onOpenUploadMedia = {
              bottomBarUIState.openExtraBar(ChatBottomBarExtra.MEDIA)
            },
            onStartMicrophone = {
              bottomBarUIState.showMicButton()
            },
            navigateToAssistantStore = {
              onNavigateToAssistantStore()
            },
            lastSessionTitle = lastSession?.unifiedTitle,
            onNavigateToLastSession = {
              if (lastSession != null) {
                onNavigateDetail(lastSession!!.id.toHexString())
              }
            }
          )
        }
      }
    }
  }

  // show token dialog
  if (showTokenDialog) {
    AlertDialog(
      icon = {
        Icon(
          ImageVector.vectorResource(R.drawable.ic_secret_files_icon),
          contentDescription = null,
          Modifier.size(25.dp)
        )
      },
      title = {
        Text(text = "前往设置界面设置 Token 以继续使用")
      },
      text = {
        Text(text = "你还没有设置 Token")
      },
      onDismissRequest = {
        showTokenDialog = false
      },
      confirmButton = {
        Button(
          onClick = {
            showTokenDialog = false
            onOpenUserProfile()
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
          onClick = { showTokenDialog = false },
          shape = MaterialTheme.shapes.medium
        ) {
          Text(stringResource(R.string.label_cancel))
        }
      }
    )
  }

  val idxToVideoPlayer: MutableMap<Int, ExoPlayer> = remember {
    mutableMapOf()
  }

  LaunchedEffect(previewerState.currentPage) {
    if (mediaPreviewList.isEmpty()) {
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
        val item = mediaPreviewList[previewerState.currentPage]
        if (item.mediaItem is VideoMediaItem) {
          // PASS
        } else {
          // 关闭预览组件
          previewerState.close()
        }
      }
    }),
    imageLoader = { index ->
      val item = mediaPreviewList[index]
      val painter = if (item.mediaItem is ImageMediaItem) {
        rememberAsyncImagePainter(
          item.mediaItem.url,
        )
      } else {
        rememberAsyncImagePainter(
          ImageRequest.Builder(context)
            .data(item.mediaItem.url)
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
      val mediaItem = mediaPreviewList[idx]
      var mounted = false

      // 单独设置每一页的背景颜色
      Box {
        // 通过调用页面获取imageLoader的状态
        mounted = innerPage()

        // TODO: show helpText when controller hide, otherwise hide helpText
        if (mediaItem.mediaItem is VideoMediaItem) {
          VideoPlayer(
            mediaItems = listOf(
              VideoPlayerMediaItem.StorageMediaItem(
                storageUri = mediaItem.mediaItem.uri,
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
        val plugin = remember {
          // TODO: 这里创建 plugin 是否正确？考虑 viewmodel 中完成
          val des = BuiltInPlugins.plugins.firstOrNull { it.name == mediaItem.plugin?.id }
          des?.create()
        }

        TopAppBar(
          title = {
            plugin?.PreviewLeadingTopContent(mediaItem)
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            actionIconContentColor = Color.White,
          ),
          actions = {
            if (mediaItem.mediaItem !is VideoMediaItem || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              FilledIconButton(
                onClick = { downloadMedia(mediaItem.mediaItem) },
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
              onClick = { shareMedia(mediaItem.mediaItem) },
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

        if (mediaItem.description != null && mediaItem.mediaItem !is VideoMediaItem) {
          BottomAppBar(
            Modifier.align(Alignment.BottomCenter),
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
          ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
              Text(
                mediaItem.description,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis
              )
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

