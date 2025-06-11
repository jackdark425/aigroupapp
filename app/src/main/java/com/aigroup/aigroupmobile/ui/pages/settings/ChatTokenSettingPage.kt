package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.utils.clearToken
import com.aigroup.aigroupmobile.data.utils.hasSetToken
import com.aigroup.aigroupmobile.data.utils.setToken
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.section
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenSettingPageInner(
  tokenPreferences: AppPreferences.Token = AppPreferences.Token.getDefaultInstance(),
  onUpdateToken: (ChatServiceProvider, String) -> Unit = { _, _ -> },
  onClearToken: (ChatServiceProvider) -> Unit = {},
  onBack: () -> Unit = {}
) {
  // 筛选已启用的服务
  val serviceSetup = ChatServiceProvider.entries.filter { tokenPreferences.hasSetToken(it) }
  // 筛选剩余可用的服务
  val serviceAvailable = ChatServiceProvider.entries.filterNot { it in serviceSetup }

  // 禁用服务提供商
  fun disableService(service: ChatServiceProvider) {
    onClearToken(service)
  }

  // 设置服务提供商
  fun setupService(service: ChatServiceProvider, token: String) {
    onUpdateToken(service, token)
  }

  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.label_set_chat_service_token), fontWeight = FontWeight.SemiBold) },
        // back button
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    },
    containerColor = AppCustomTheme.colorScheme.groupedBackground
  ) { innerPadding ->
    Box(
      Modifier
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 0.dp)
    ) {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 150.dp)
      ) {
        section(context.getString(R.string.label_chat_service_enable), serviceSetup, key = { it.name + "-enabled" }, stickyHeader = false) { service ->
          SectionListItem(
            title = service.displayName,
            description = service.description,
            icon = ImageVector.vectorResource(service.logoIconId),
            iconBg = service.backColor,
            iconTint = if (service == ChatServiceProvider.OFFICIAL) null else Color.Unspecified, // TODO: ??
            trailingContent = {
              Switch(
                checked = true,
                onCheckedChange = { disableService(service) },
                modifier = Modifier.scale(0.6f),
                colors = SwitchDefaults.colors(
                  checkedTrackColor = AppCustomTheme.colorScheme.primaryAction
                )
              )
            }
          )
        }

        if (serviceSetup.isNotEmpty()) {
          item {
            Spacer(Modifier.height(16.dp))
          }
        }

        section(context.getString(R.string.label_chat_service_available), serviceAvailable, key = { it.name + "-available" }, stickyHeader = false) { service ->
          SectionListItem(
            title = service.displayName,
            description = service.description,
            icon = ImageVector.vectorResource(service.logoIconId),
            iconBg = service.backColor,
            iconTint = if (service == ChatServiceProvider.OFFICIAL) null else Color.Unspecified,
            modalContent = {
              createInput(
                label = "${service.displayName} Token",
                secret = true,
                hideOnSubmit = true,
                autoFocus = true,
                showConfirmButton = true,
                onSubmit = {
                  setupService(service, it)
                }
              )
            }
          )
        }

      }
    }
  }
}

@Composable
fun ChatTokenSettingPage(
  viewModel: SettingsViewModel = hiltViewModel(),
  onBack: () -> Unit = {}
) {
  // TODO: 使用 main activity 初始化读取到的作为 default ？或者延长 SettingsViewModel
  val token by viewModel.preferences.map { it.token }.collectAsStateWithLifecycle(
    AppPreferences.Token.getDefaultInstance()
  )

  TokenSettingPageInner(token, viewModel::updateTokenPreferences, viewModel::clearTokenPreferences, onBack)
}

@Preview(showSystemUi = true)
@Composable
fun ChatTokenSettingPagePreview() {
  var tokenPreferences by remember {
    mutableStateOf(AppPreferences.Token.newBuilder().apply {
      generic = "generic_token"
    }.build())
  }

  AIGroupAppTheme {
    TokenSettingPageInner(
      tokenPreferences = tokenPreferences,
      onUpdateToken = { p, token ->
        tokenPreferences = tokenPreferences.toBuilder().setToken(p, token).build()
      },
      onClearToken = { p ->
        tokenPreferences = tokenPreferences.toBuilder().clearToken(p).build()
      }
    )
  }
}