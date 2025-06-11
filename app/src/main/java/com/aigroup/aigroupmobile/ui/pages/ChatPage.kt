@file:OptIn(ExperimentalFoundationApi::class, ExperimentalEncodingApi::class)

package com.aigroup.aigroupmobile.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.botSenders
import com.aigroup.aigroupmobile.data.models.lastModifyDateTime
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.sessionDisplaySubtitle
import com.aigroup.aigroupmobile.data.models.sessionDisplayTitle
import com.aigroup.aigroupmobile.ui.pages.assistant.components.AssistantLocalItem
import com.aigroup.aigroupmobile.ui.pages.chat.conversation.components.ConversationAppBar
import com.aigroup.aigroupmobile.ui.pages.chat.conversation.components.ConversationItem
import com.aigroup.aigroupmobile.ui.pages.chat.detail.components.ChatAppBar
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.viewmodels.AssistantViewModel
import com.aigroup.aigroupmobile.viewmodels.ChatConversationViewModel
import com.aigroup.aigroupmobile.viewmodels.ConversationFilter
import com.aigroup.aigroupmobile.viewmodels.UserProfileViewModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
private fun AssistantEmptyPage(modifier: Modifier = Modifier, navigateToAssistantBuilder: () -> Unit = {}) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "No Assistants",
      style = MaterialTheme.typography.titleMedium,
      color = AppCustomTheme.colorScheme.primaryLabel,
      modifier = Modifier.padding(horizontal = 15.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "You can add assistants from the store",
      style = MaterialTheme.typography.labelSmall,
      color = AppCustomTheme.colorScheme.secondaryLabel,
      modifier = Modifier.padding(horizontal = 15.dp)
    )

    // create assistant button
    Spacer(modifier = Modifier.height(20.dp))
    Button(
      onClick = {
        navigateToAssistantBuilder()
      },
      shape = MaterialTheme.shapes.large,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = AppCustomTheme.colorScheme.primaryAction
      )
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Add,
          "",
          Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("创建助手")
      }
    }
  }
}

@Preview
@Composable
private fun AssistantEmptyPagePreview() {
  AIGroupAppTheme {
    AssistantEmptyPage()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun ChatPage(
  chatId: String? = null,
  onNavigateDetail: (String) -> Unit,
  onGotoAssistantsStore: () -> Unit, // TODO: rename
  onNavigateAssistantBuilder: () -> Unit,

  onNavigateDetailSetting: (String) -> Unit,
  onNavigateAssistantSetting: (BotAssistant) -> Unit,

  onHideDrawer: () -> Unit,

  conversationViewModel: ChatConversationViewModel = hiltViewModel(),
  userProfileViewModel: UserProfileViewModel = hiltViewModel(),
  assistantViewModel: AssistantViewModel = hiltViewModel(),

  splitMode: Boolean = false,
) {
  val coroutineScope = rememberCoroutineScope()

  val userProfile by userProfileViewModel.profile.observeAsState()

  val conversations by conversationViewModel.normalSessions.observeAsState()
  val pinnedConversations by conversationViewModel.pinnedSessions.observeAsState()
  val filter by conversationViewModel.filter.collectAsStateWithLifecycle()

//  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val mainContentState = rememberLazyListState()

  val localAssistants by assistantViewModel.localAssistants.observeAsState()

  LaunchedEffect(pinnedConversations?.count()) {
    val size = pinnedConversations?.count() ?: 0
    if (size > 0) {
      mainContentState.animateScrollToItem(0)
    }
  }

  fun onCreateSession() {
    coroutineScope.launch {
      val session = conversationViewModel.createEmptySessionIfNotExists()
      onNavigateDetail(session.id.toHexString())
    }
  }

  fun onStartChat(assistant: BotAssistant) {
    coroutineScope.launch {
      val session = conversationViewModel.createEmptySessionWithAssistant(assistant)
      onNavigateDetail(session.id.toHexString())
    }
  }

  val hazeState = remember { HazeState() }

  var selectedMode by remember { mutableStateOf(ConversationAppBar.Mode.Conversation) }

  Scaffold(
    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = AppCustomTheme.colorScheme.groupedBackground,
    topBar = {
      Column {
        ConversationAppBar.TopAppBar(
          modifier = Modifier.hazeChild(hazeState),
          scrollBehavior = scrollBehavior,
          avatar = userProfile?.avatar,
          onHideDrawer = { onHideDrawer() },
          showHideDrawerButton = !splitMode,
          selectedMode = selectedMode,
          onModeChange = { selectedMode = it },
        )
        ConversationAppBar.TopAppBarSurface(
          scrollBehavior = scrollBehavior,
          modifier = Modifier
            .hazeChild(hazeState)
            .padding(bottom = 8.dp),
        ) {
          ConversationAppBar.EmbeddedSearchBar(
            scrollBehavior = scrollBehavior,
            onSearch = {
              when (selectedMode) {
                ConversationAppBar.Mode.Conversation -> {
                  conversationViewModel.setSearch(it)
                }
                ConversationAppBar.Mode.Assistants -> {
                  assistantViewModel.setSearch(it)
                }
              }
            },
            onTop = {
              coroutineScope.launch {
                mainContentState.animateScrollToItem(0)
                // FIXME: not show appbar
              }
            },
            selectedMode = selectedMode,
          )
        }
      }
    },
  ) { innerPadding ->
    Box(
      Modifier
        .fillMaxSize()
        .haze(
          state = hazeState,
          style = HazeDefaults.style(
            backgroundColor = AppCustomTheme.colorScheme.groupedBackground,
            blurRadius = 150.dp,
            noiseFactor = 20f,
          )
        )
    ) {

      LazyColumn(
        state = mainContentState,
        contentPadding = PaddingValues(
          top = 3.dp + innerPadding.calculateTopPadding(),
          bottom = 100.dp + innerPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier,
      ) {
        when (selectedMode) {
          ConversationAppBar.Mode.Conversation -> {
            if (pinnedConversations?.isNotEmpty() == true) {
              item(key = "pinned-title") {
                Box(
                  Modifier.animateItem()
                ) {
                  Column {
                    Text(
                      text = stringResource(R.string.label_pinned_sessions),
                      style = MaterialTheme.typography.labelMedium,
                      color = AppCustomTheme.colorScheme.secondaryLabel,
                      modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .padding(top = 5.dp)
                    )
                  }
                }
              }

              items(pinnedConversations!!, key = { "${it.id.toHexString()}-pin" }) {
                ConversationItem(
                  modifier = Modifier
                    .padding(horizontal = 9.dp)
                    .animateItem(),
                  title = it.sessionDisplayTitle,
                  subtitle = it.sessionDisplaySubtitle,
                  time = it.lastModifyDateTime.readableStr,
                  isPinned = true,
                  assistant = it.primaryBotSender?.botSender?.assistant,
                  onClick = {
                    onNavigateDetail(it.id.toHexString())
                  },
//                  model = it.primaryBot?.model ?: ModelCode.Unknown,
                  botSenders = it.botSenders,
                  onCancelPin = {
                    conversationViewModel.unpinChatSession(it)
                  },
                  onDeleteSelf = {
                    coroutineScope.launch {
                      val next = conversationViewModel.deleteChatSession(it)
                      onNavigateDetail(next.id.toHexString())
                    }
                  },
                  onCopySelf = {
                    conversationViewModel.cloneChatSession(it)
                  },
                  onShowSettings = {
                    onNavigateDetailSetting(it.id.toHexString())
                  },
                  selected = it.id.toHexString() == chatId,
                )
              }

              item {
                Spacer(modifier = Modifier.height(8.dp))
              }
            }

            // TODO: support item type in LazyColumn in everywhere in projects.
            item(key = "filter") {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 18.dp)
                  .animateItem()
              ) {
                for (item in ConversationFilter.entries) {
                  FilterChip(
                    selected = filter == item,
                    onClick = {
                      conversationViewModel.setFilter(item)
                    },
                    label = { Text(item.display, style = MaterialTheme.typography.labelMedium) },
                    border = BorderStroke(1.dp, Color.Transparent),
                    colors = FilterChipDefaults.filterChipColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                      selectedContainerColor = AppCustomTheme.colorScheme.primaryAction,
                      selectedLabelColor = AppCustomTheme.colorScheme.onPrimaryAction,
                      labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                }
              }
            }

            if (conversations?.isNotEmpty() == true) {
              items(conversations!!, key = { it.id.toHexString() }) {
                ConversationItem(
                  modifier = Modifier
                    .padding(horizontal = 9.dp)
                    .animateItem(),
                  title = it.sessionDisplayTitle,
                  subtitle = it.sessionDisplaySubtitle,
                  time = it.lastModifyDateTime.readableStr,
                  assistant = it.primaryBotSender?.botSender?.assistant,
                  isPinned = false,
                  selected = it.id.toHexString() == chatId,
                  onClick = {
                    onNavigateDetail(it.id.toHexString())
                  },
//                  model = it.primaryBot?.model ?: ModelCode.Unknown,
                  botSenders = it.botSenders,
                  onPin = {
                    conversationViewModel.pinChatSession(it)
                  },
                  onDeleteSelf = {
                    coroutineScope.launch {
                      val next = conversationViewModel.deleteChatSession(it)
                      onNavigateDetail(next.id.toHexString())
                    }
                  },
                  onCopySelf = {
                    conversationViewModel.cloneChatSession(it)
                  },
                  onShowSettings = {
                    onNavigateDetailSetting(it.id.toHexString())
                  },
                )
              }
            }
          }

          ConversationAppBar.Mode.Assistants -> {
            if (localAssistants.isNullOrEmpty()) {
              item {
                AssistantEmptyPage(
                  modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight()
                    .animateItem(),
                  navigateToAssistantBuilder = onNavigateAssistantBuilder
                )
              }
            } else {
              items(localAssistants!!, key = { it.id.toHexString() }) {
                AssistantLocalItem(
                  modifier = Modifier
                    .padding(horizontal = 9.dp)
                    .animateItem(),
                  assistant = it,

                  onDeleteSelf = {
                    assistantViewModel.deleteLocalAssistant(it)
                  },
                  onShowSettings = {
                    onNavigateAssistantSetting(it)
                  },
                  onStartChat = {
                    onStartChat(it)
                  },

                  onClick = {
                    onNavigateAssistantSetting(it)
                  },
                )
              }
            }
          }
        }
      }

      AnimatedContent(
        selectedMode,
        Modifier
          .align(Alignment.BottomCenter)
          .safeDrawingPadding()
          .padding(bottom = 10.dp),
      ) {
        when (it) {
          ConversationAppBar.Mode.Conversation -> {
            Button(
              modifier = Modifier,
              onClick = { onCreateSession() },
              shape = MaterialTheme.shapes.large,
              colors = ButtonDefaults.buttonColors(
                containerColor = AppCustomTheme.colorScheme.primaryAction,
                contentColor = AppCustomTheme.colorScheme.onPrimaryAction
              )
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_bubble_plus_icon_legacy),
                  "",
                  Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.label_button_create_session))
              }
            }
          }

          ConversationAppBar.Mode.Assistants -> {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              Button(
                modifier = Modifier,
                onClick = { onGotoAssistantsStore() },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                  containerColor = AppCustomTheme.colorScheme.primaryAction,
                  contentColor = AppCustomTheme.colorScheme.onPrimaryAction
                )
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    ImageVector.vectorResource(R.drawable.ic_bubble_plus_icon_legacy),
                    "",
                    Modifier.size(25.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(stringResource(R.string.label_button_assistant_store))
                }
              }

              if (!localAssistants.isNullOrEmpty()) {
                Button(
                  onClick = {
                    onNavigateAssistantBuilder()
                  },
                  shape = MaterialTheme.shapes.large,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = AppCustomTheme.colorScheme.primaryAction
                  )
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.Add,
                      "",
                      Modifier.size(25.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("创建助手")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}