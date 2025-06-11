package com.aigroup.aigroupmobile.connect.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.connect.intro.SerperIntro
import com.aigroup.aigroupmobile.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.map

// TODO: 考虑根据 PlatformIntro 来动态化生成该 package 下的页面

@Composable
fun SerperTokenPage(
  onBack: () -> Unit = {},
  viewModel: SettingsViewModel = hiltViewModel()
) {
  val hasToken by viewModel.preferences.map { it.serviceToken.hasSerper() }.collectAsStateWithLifecycle(false)
  val token by viewModel.preferences.map { it.serviceToken.serper }.collectAsStateWithLifecycle("")

  fun updateToken(token: String?) {
    viewModel.updateServiceTokenPreferences {
      if (token == null) {
        clearSerper()
      } else {
        setSerper(token)
      }.build()
    }
  }

  TokenSetupPage(
    intro = SerperIntro,
    onBack = onBack,
    enable = hasToken,
    onDisable = { updateToken(null) },
    onTokenUpdate = ::updateToken
  )
}
