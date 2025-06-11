package com.aigroup.aigroupmobile.ui.pages.assistant

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import com.aigroup.aigroupmobile.data.utils.hasSetToken
import com.aigroup.aigroupmobile.dataStore
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.BotAvatarEmoji
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.MultiLineField
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import com.aigroup.aigroupmobile.utils.system.OpenExternal
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.viewmodels.AssistantBuilderViewModel
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.mongodb.kbson.ObjectId

private val PROMPT_DEMO = """
你是一位总是以苏格拉底式回应的导师。你永远不会给学生答案，但总是试图提出正确的问题来帮助他们学会独立思考。
您应该始终根据学生的兴趣和知识调整您的问题，将问题分解为更简单的部分，直到适合他们的水平。
""".trim()

sealed interface AssistantBuilderAvatar {
  data class Emoji(val emoji: String) : AssistantBuilderAvatar
  data class LocalImage(val img: ImageMediaItem) : AssistantBuilderAvatar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantAvatarBuilder(
  avatar: AssistantBuilderAvatar,
  generateAvatar: () -> Unit = {},
  generatingAvatar: Boolean = false,
  onUpdateAvatar: (AssistantBuilderAvatar) -> Unit = {},
) {
  var isModalBottomSheetVisible by remember {
    mutableStateOf(false)
  }

  if (isModalBottomSheetVisible) {
    ModalBottomSheet(
      sheetState = rememberModalBottomSheetState(),
      onDismissRequest = { isModalBottomSheetVisible = false },
      modifier = Modifier.safeDrawingPadding()
    ) {
      // TODO: mark 嵌套滚动
      AndroidView(
        factory = { context ->
          EmojiPickerView(context).apply {
            setOnEmojiPickedListener {
              onUpdateAvatar(AssistantBuilderAvatar.Emoji(it.emoji))
              isModalBottomSheetVisible = false
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }

  Column(
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    MediaSelector.TakeGalleryButton(
      type = MediaSelector.MediaType.Image,
      onMediaSelected = { img ->
        if (img == null) return@TakeGalleryButton
        val newAvatar = AssistantBuilderAvatar.LocalImage(img as ImageMediaItem)
        onUpdateAvatar(newAvatar)
      }
    ) {
      // TODO: using bot avatar
      Box(
        modifier = Modifier
          .size(100.dp)
          .padding(16.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
          .clickable {
            if (avatar is AssistantBuilderAvatar.LocalImage) {
              this.onClick()
            } else {
              isModalBottomSheetVisible = true
            }
          }
      ) {
        when (avatar) {
          is AssistantBuilderAvatar.Emoji -> {
            Text(
              text = avatar.emoji,
              fontSize = 30.sp,
              modifier = Modifier.align(Alignment.Center)
            )
          }

          is AssistantBuilderAvatar.LocalImage -> {
            val img = avatar.img
            Image(
              painter = rememberAsyncImagePainter(img.url),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(100.dp)
            )
          }
        }
      }
    }

    // emoji select button (round text button)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MediaSelector.TakeGalleryButton(
        type = MediaSelector.MediaType.Image,
        onMediaSelected = { img ->
          if (img == null) return@TakeGalleryButton
          val newAvatar = AssistantBuilderAvatar.LocalImage(img as ImageMediaItem)
          onUpdateAvatar(newAvatar)
        }
      ) {
        TextButton(
          {
            if (avatar is AssistantBuilderAvatar.Emoji) {
              this.onClick()
            } else {
              isModalBottomSheetVisible = true
            }
          },
          shape = MaterialTheme.shapes.medium,
          contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp),
          colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = AppCustomTheme.colorScheme.primaryAction,
          )
        ) {
          Text(
            if (avatar is AssistantBuilderAvatar.Emoji) {
              "上传图片"
            } else {
              "使用 Emoji"
            },
            fontWeight = FontWeight.Bold
          )
        }
      }
      TextButton(
        {
          generateAvatar()
        },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp),
        colors = ButtonDefaults.textButtonColors(
          containerColor = MaterialTheme.colorScheme.background,
          contentColor = AppCustomTheme.colorScheme.primaryAction,
        ),
        enabled = !generatingAvatar
      ) {
        if (generatingAvatar) {
          CircularProgressIndicator(
            color = AppCustomTheme.colorScheme.primaryAction,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
          )
        } else {
          Text("AI 生成形象", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun BotAvatar(avatar: AssistantBuilderAvatar, size: Dp = 30.dp) {
  when (avatar) {
    is AssistantBuilderAvatar.Emoji -> {
      BotAvatarEmoji(avatar.emoji, size)
    }
    is AssistantBuilderAvatar.LocalImage -> {
      BotAvatar(avatar.img, size)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AssistantBuilderPageInner(
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,

  knowledgeDocs: List<DocumentItem> = emptyList(),
  indexingDocIds: List<ObjectId> = emptyList(),

  onGenerateAvatar: (title: String, description: String, update: (AssistantBuilderAvatar) -> Unit) -> Unit = { _, _, _ -> },
  onGenerateDescription: suspend (title: String) -> String? = { null },
  onGenerateSystemPrompt: suspend (title: String, description: String) -> String? = { _, _ -> null },
  onGenerateStartPrompts: suspend (title: String, description: String) -> List<String> = { _, _ -> emptyList() },

  onAddKnowledgeDocument: (DocumentMediaItem) -> Unit = {},
  onRemoveKnowledgeDocument: (DocumentItem) -> Unit = {},

  onBuildAssistant: suspend (RemoteAssistant.Configuration.Builder, RemoteAssistant.Metadata.Builder, List<String>) -> Unit = { _, _, _ -> },
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var avatar: AssistantBuilderAvatar by remember {
    mutableStateOf(AssistantBuilderAvatar.Emoji("🤖"))
  }
  var assistantTitle by remember { mutableStateOf("") }
  var assistantDescription by remember { mutableStateOf(TextFieldValue("")) }
  var roleDescription by remember { mutableStateOf(TextFieldValue("")) }

  val isInspection = LocalInspectionMode.current
  var startPrompts: List<TextFieldValue> by remember {
    if (isInspection) {
      mutableStateOf(
        listOf(
          TextFieldValue("你好，我是你的助手"),
          TextFieldValue("我可以帮助你解决问题"),
          TextFieldValue("请问你有什么问题需要帮助吗？"),
        )
      )
    } else {
      mutableStateOf(emptyList())
    }
  }
  var startPrompt by remember { mutableStateOf(TextFieldValue("")) }


  // TODO: using loading status
  var generatingAvatar by remember { mutableStateOf(false) }
  var generatingInfo by remember { mutableStateOf(false) }

  val canBuild by remember {
    derivedStateOf {
      assistantTitle.isNotBlank() && assistantDescription.text.isNotBlank() && roleDescription.text.isNotBlank() &&
          indexingDocIds.isEmpty() && !generatingInfo
    }
  }

  suspend fun buildAssistant() {
    val modelCode = context.dataStore.data.map { it.defaultModelCode }.first(); // TODO: support config in this page

    val config = RemoteAssistant.Configuration.Builder()
      .role(roleDescription.text)
      .preferredModelCode(modelCode)
    val metadata = RemoteAssistant.Metadata.Builder()
      .title(assistantTitle)
      .category("自定义") // TODO: fill category and tags
      .tags(emptyList())
      .description(assistantDescription.text)

    when (avatar) {
      is AssistantBuilderAvatar.Emoji -> {
        metadata.avatarEmoji((avatar as AssistantBuilderAvatar.Emoji).emoji)
      }

      is AssistantBuilderAvatar.LocalImage -> {
        metadata.avatarLink((avatar as AssistantBuilderAvatar.LocalImage).img.url)
      }
    }

    onBuildAssistant(config, metadata, startPrompts.map { it.text })

    withContext(Dispatchers.Main) {
      Toast.makeText(context, "助手创建成功", Toast.LENGTH_SHORT).show()
      onDone?.invoke()
    }
  }

  Scaffold(
    modifier = Modifier
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = AppCustomTheme.colorScheme.groupedBackground,
        ),
        title = {
          // TODO: add avatar here with nest scrolling
//          Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
//            BotAvatar(
//              avatar = avatar,
//              size = 25.dp
//            )
//            Text("创建助手", style = MaterialTheme.typography.titleMedium)
//          } // i18n TODO
          Text("创建助手", style = MaterialTheme.typography.titleMedium)
        },
        navigationIcon = {
          if (onBack != null) {
            IconButton(onClick = onBack) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                Modifier.size(20.dp)
              )
            }
          }
        },
        actions = {
          TextButton(
            onClick = {
              if (assistantTitle.isBlank()) {
                Toast.makeText(context, "请先填写助手名称", Toast.LENGTH_SHORT).show()
              } else {
                generatingInfo = true

                // TODO: manage in viewModel
                coroutineScope.launch {
                  // TODO: enhance this logic
                  val hasToken = context.dataStore.data.map {
                    // TODO: can be configure
                    it.token.hasSetToken(ChatServiceProvider.OFFICIAL)
                  }.first()
                  if (!hasToken) {
                    Toast.makeText(context, "请先设置 API Token", Toast.LENGTH_SHORT).show()
                    generatingInfo = false
                    return@launch
                  }

                  if (assistantDescription.text.isBlank()) {
                    val generated = onGenerateDescription(assistantTitle)
                    if (generated != null) {
                      assistantDescription = TextFieldValue(generated)
                    } else {
                      Toast.makeText(context, "一键完善失败", Toast.LENGTH_SHORT).show()
                    }
                  }
                  val generated = onGenerateSystemPrompt(assistantTitle, assistantDescription.text)
                  if (generated != null) {
                    roleDescription = TextFieldValue(generated)
                  } else {
                    Toast.makeText(context, "一键完善失败", Toast.LENGTH_SHORT).show()
                  }

                  val generatedStartPrompts = onGenerateStartPrompts(assistantTitle, assistantDescription.text)
                  if (generatedStartPrompts.isNotEmpty()) {
                    startPrompts = generatedStartPrompts.map { TextFieldValue(it) }
                  } else {
                    Toast.makeText(context, "一键完善开场白失败", Toast.LENGTH_SHORT).show()
                  }

                  generatingInfo = false
                }

              }
            },
            shape = MaterialTheme.shapes.medium,
            enabled = assistantTitle.isNotBlank() && !generatingInfo,
          ) {
            if (generatingInfo) {
              CircularProgressIndicator(
                color = AppCustomTheme.colorScheme.primaryAction,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
              )
            } else {
              Text("一键完善", fontWeight = FontWeight.Bold)
            }
          }
        }
      )
    },
    bottomBar = {
      val imeShow = WindowInsets.isImeVisible

      // confirm button
      AnimatedVisibility(!imeShow, modifier = Modifier.fillMaxWidth()) {
        Button(
          enabled = canBuild,
          onClick = {
            coroutineScope.launch {
              buildAssistant()
            }
          },
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = AppCustomTheme.colorScheme.primaryAction
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
          Text("创建助手", style = MaterialTheme.typography.titleMedium) // i18n TODO
        }
      }
    },
    containerColor = AppCustomTheme.colorScheme.groupedBackground,
  ) { innerPadding ->
    LazyColumn(
      contentPadding = innerPadding,
      verticalArrangement = Arrangement.spacedBy(13.dp),
      modifier = modifier.padding(horizontal = 16.dp),
    ) {
      item {
        AssistantAvatarBuilder(avatar, generatingAvatar = generatingAvatar, generateAvatar = {
          val canGenerate = assistantTitle.isNotBlank() || assistantDescription.text.isNotBlank()
          if (!canGenerate) {
            Toast.makeText(context, "请先填写助手名称或简介", Toast.LENGTH_SHORT).show()
          } else {
            generatingAvatar = true
            onGenerateAvatar(assistantTitle, assistantDescription.text) {
              avatar = it
              generatingAvatar = false
            }
          }
        }) {
          avatar = it
        }
      }

      item {
        BasicTextField(
          value = assistantTitle,
          onValueChange = { assistantTitle = it },
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          modifier = Modifier
            .height(45.dp)
            .fillMaxWidth()
            .clearFocusOnKeyboardDismiss(),
          singleLine = true,
        ) { innerTextField ->
          TextFieldDefaults.DecorationBox(
            value = assistantTitle,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            placeholder = {
              Text(
                text = "给助手起个名字",
                color = AppCustomTheme.colorScheme.secondaryLabel,
                style = MaterialTheme.typography.bodyMedium
              )
            },
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
              top = 0.dp,
              bottom = 0.dp
            ),
            leadingIcon = {
              Text(
                "助手名称",
                color = AppCustomTheme.colorScheme.primaryLabel,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .padding(start = 10.dp, end = 12.dp)
              )
            },
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              cursorColor = AppCustomTheme.colorScheme.primaryAction,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
          )
        }
      }

      item {
        Text(
          "助手简介",
          style = MaterialTheme.typography.labelMedium.copy(
            color = AppCustomTheme.colorScheme.secondaryLabel,
          ),
          modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
        )

        MultiLineField(
          value = assistantDescription,
          onValueChange = { assistantDescription = it },
          placeholder = "简单介绍助手功能",
          shape = MaterialTheme.shapes.medium,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          innerFieldContentAlignment = Alignment.TopStart,
          modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnKeyboardDismiss(),
        )
      }

      item {
        Spacer(Modifier.height(13.dp))

        Text(
          "角色描述",
          style = MaterialTheme.typography.labelMedium.copy(
            color = AppCustomTheme.colorScheme.secondaryLabel,
          ),
          modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
        )

        MultiLineField(
          value = roleDescription,
          onValueChange = { roleDescription = it },
          placeholder = PROMPT_DEMO,
          shape = MaterialTheme.shapes.medium,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          innerFieldContentAlignment = Alignment.TopStart,
          modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnKeyboardDismiss(),
        )
      }

      // TODO: using section() so that we can using item animation
      item {
        Spacer(Modifier.height(8.dp))

        Text(
          "知识库",
          style = MaterialTheme.typography.labelMedium.copy(
            color = AppCustomTheme.colorScheme.secondaryLabel,
          ),
          modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
        )

        // 知识库
        SectionListSection(
          "知识库",
          topSpacing = 0.dp,
          showTitle = false,
          sectionShape = MaterialTheme.shapes.medium,
        ) {
          MediaSelector.TakeDocFileButton(
            onMediaSelected = {
              println("TakeFileButton onMediaSelected: $it")
              if (it is DocumentMediaItem) {
                // TODO: consider return specified type of MediaItem in MediaSelector functions
                onAddKnowledgeDocument(it)
              }
            }
          ) {
            SectionListItem(
              icon = ImageVector.vectorResource(R.drawable.ic_add_docs),
              title = "添加文档", // i18n TODO
              noIconBg = true,
              iconModifier = Modifier.size(20.dp),
              onClick = { this.onClick() },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
              titleTextStyle = MaterialTheme.typography.bodyMedium
            )
          }

          if (knowledgeDocs.isNotEmpty()) {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.surfaceDim
            )
          }

          knowledgeDocs.forEachIndexed { index, doc ->
            // 右滑删除
            val deleteAction = SwipeAction(
              icon = rememberVectorPainter(CssGgIcons.Trash),
              background = MaterialTheme.colorScheme.errorContainer,
              onSwipe = {
                onRemoveKnowledgeDocument(doc)
              }
            )

            SwipeableActionsBox(
              endActions = if (indexingDocIds.contains(doc.id)) emptyList() else listOf(deleteAction),
              backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
              SectionListItem(
                // TODO: mime to icon
                icon = ImageVector.vectorResource(R.drawable.ic_doc_icon),
                title = doc.document!!.title,
                loading = indexingDocIds.contains(doc.id),
                description = if (indexingDocIds.contains(doc.id)) {
                  "正在索引"
                } else if (doc.knowledgeDocId == null) {
                  "未索引，点击立即索引"
                } else {
                  "已经索引"
                },// i18n TODO
                noIconBg = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                titleTextStyle = MaterialTheme.typography.bodyMedium,
                iconModifier = Modifier.size(20.dp),
                onClick = {
                  coroutineScope.launch {
                    OpenExternal.openDocMediaItemExternal(context, doc.document!!)
                  }
                }
              )
            }

            if (index < knowledgeDocs.size - 1) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceDim
              )
            }
          }
        }
      }

      item {
        // 开场白
        Spacer(Modifier.height(8.dp))

        Row(
          modifier = Modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 4.dp),
        ) {
          Text(
            "开场白",
            style = MaterialTheme.typography.labelMedium.copy(
              color = AppCustomTheme.colorScheme.secondaryLabel,
            ),
          )
        }

        for ((idx, prompt) in startPrompts.withIndex()) {
          val baseShape = MaterialTheme.shapes.medium
          val shape = if (idx == 0) {
            baseShape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
          } else {
            RectangleShape
          }

          MultiLineField(
            value = prompt,
            onValueChange = {
              startPrompts = startPrompts.toMutableList().also { list ->
                list[idx] = it
              }
            },
            shape = shape,
            placeholder = "修改开场白",
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
            modifier = Modifier
              .fillMaxWidth()
              .clearFocusOnKeyboardDismiss(),
          )
        }

        val fieldShape = if (startPrompts.isEmpty()) {
          MaterialTheme.shapes.medium
        } else {
          MaterialTheme.shapes.medium.copy(
            topStart = CornerSize(0.dp),
            topEnd = CornerSize(0.dp),
          )
        }
        MultiLineField(
          value = startPrompt,
          onValueChange = { startPrompt = it },
          shape = fieldShape,
          placeholder = "添加开场白",
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          action = {
            IconButton(
              onClick = {
                startPrompts = startPrompts + startPrompt
                startPrompt = TextFieldValue("")
              },
              enabled = startPrompt.text.isNotBlank()
            ) {
              Icon(Icons.Default.Add, "", Modifier.size(20.dp))
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnKeyboardDismiss(),
        )

      }

      item {
        Spacer(Modifier.height(150.dp))
      }
    }
  }
}

@Composable
fun AssistantBuilderPage(
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
  viewModel: AssistantBuilderViewModel = hiltViewModel(),
) {
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val knowledgeDocs by viewModel.knowledgeDocs.collectAsStateWithLifecycle(emptyList())
  val indexingDocIds by viewModel.indexingDocIds.collectAsStateWithLifecycle(emptyList())
  var showExitDialog by remember { mutableStateOf(false) }

  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = {
        showExitDialog = false
      },
      title = { Text(text = "退出") },
      text = { Text(text = "确定要退出构建自定义助手吗？你的更改将无法保存。") },
      dismissButton = {
        Button(
          onClick = { showExitDialog = false },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Text(
            text = stringResource(R.string.label_cancel),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showExitDialog = false
            onBack?.invoke()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text(
            text = stringResource(R.string.label_confirm),
          )
        }
      }
    )
  }

  BackHandler {
    showExitDialog = !showExitDialog
  }

  Box {
    AssistantBuilderPageInner(
      modifier = modifier,
      onBack = { showExitDialog = true },
      onDone = { onBack?.invoke() },

      knowledgeDocs = knowledgeDocs,
      indexingDocIds = indexingDocIds,

      onGenerateAvatar = { title, description, update ->
        coroutineScope.launch {
          val avatar = viewModel.generateAvatar(title, description)
          if (avatar != null) {
            update(avatar)
          } else {
            Toast.makeText(context, "生成形象失败", Toast.LENGTH_SHORT).show()
          }
        }
      },
      onGenerateDescription = viewModel::generateDescription,
      onGenerateSystemPrompt = viewModel::generateRoleDescription,
      onGenerateStartPrompts = viewModel::generateStartPrompts,
      onBuildAssistant = viewModel::buildAssistant,
      onAddKnowledgeDocument = viewModel::importKnowledgeDocument,
      onRemoveKnowledgeDocument = viewModel::removeKnowledgeDocument,
    )
  }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun AssistantBuilderPagePreview() {
  val context = LocalContext.current
  val pathManager = PathManager(context)
  CompositionLocalProvider(LocalPathManager provides pathManager) {
    AIGroupAppTheme {
      AssistantBuilderPageInner(
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}