package com.aigroup.aigroupmobile.ui.pages.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.WelcomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WelcomeModelPage(
  modifier: Modifier = Modifier,
  initial: WelcomeInitial,
  goNextWelcomePage: (WelcomeInitial) -> Unit = {},
  initializeModel: suspend (ModelCode) -> ChatSession
) {
  val coroutineScope = rememberCoroutineScope()

  var selectedModel: ModelCode? by rememberSaveable { mutableStateOf(null) }

  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

  Scaffold(modifier) { innerPadding ->
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineMedium) {
      Column(
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxSize()
          .padding(horizontal = 32.dp),
        horizontalAlignment = if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
          Alignment.Start
        } else {
          Alignment.CenterHorizontally
        }
      ) {
        Spacer(Modifier.weight(1f))

        Column {
          Text("💬")

          Spacer(Modifier.height(16.dp))

          Column(Modifier.imePadding()) {
            Text(
              stringResource(R.string.label_welcome_select_llm_model),
              fontWeight = FontWeight.SemiBold
            )
            Text(stringResource(R.string.label_welcome_select_llm_model_desc), style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(16.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.widthIn(max = 350.dp)
            ) {
              AppPreferencesDefaults.defaultFavoriteModelCodes.map { model ->
                Box(
                  modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                      if (selectedModel == model) {
                        model.tintColor
                      } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                      }
                    )
                    .clickable {
                      selectedModel = model
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .padding(end = 10.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      Modifier
                        .clip(CircleShape)
                        .background(model.tintColor.copy(alpha = 0.9f))
                        .padding(5.dp),
                    ) {
                      Icon(
                        painter = painterResource(model.iconId),
                        contentDescription = "Avatar",
                        tint = model.contentColor,
                        modifier = Modifier.size(13.dp)
                      )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                      model.toString(),
                      style = MaterialTheme.typography.titleSmall,
                      color = if (selectedModel == model) {
                        model.contentColor
                      } else {
                        MaterialTheme.colorScheme.onSurface
                      },
                      lineHeight = 11.sp
                    )
                  }
                }
              }
            }

            Spacer(Modifier.height(16.dp))

            Button(
              onClick = {
                coroutineScope.launch {
                  val emptySession = initializeModel(selectedModel!!)
                  goNextWelcomePage(initial.copy(hasFavoriteModel = true, emptySessionId = emptySession.id.toHexString()))
                }
              },
              shape = MaterialTheme.shapes.medium,
              colors = ButtonDefaults.buttonColors(
                containerColor = AppCustomTheme.colorScheme.primaryAction
              ),
              enabled = selectedModel != null
            ) {
              Text(stringResource(R.string.label_welcome_button_continue))
            }
          }
        }

        Spacer(Modifier.weight(1f))
      }
    }
  }
}

@Composable
fun WelcomeModelPage(
  modifier: Modifier = Modifier,
  initial: WelcomeInitial,
  goNextWelcomePage: (WelcomeInitial) -> Unit = {},
  viewModel: WelcomeViewModel = hiltViewModel()
) {
  WelcomeModelPage(
    modifier = modifier,
    initial = initial,
    goNextWelcomePage = goNextWelcomePage,
    initializeModel = viewModel::initializeDefaultModel
  )
}

@Preview
@Composable
private fun TestWelcomeViewModel() {
  WelcomeModelPage(
    initial = WelcomeInitial(
      hasUserProfile = false,
      hasFavoriteModel = false,
      emptySessionId = null,
      latestInitializedVersionCode = null
    ),
    goNextWelcomePage = {},
    initializeModel = { _ ->
      ChatSession()
    }
  )
}