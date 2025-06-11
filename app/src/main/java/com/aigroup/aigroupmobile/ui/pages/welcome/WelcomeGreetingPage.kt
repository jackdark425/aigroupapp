package com.aigroup.aigroupmobile.ui.pages.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.components.AppCopyRight
import com.aigroup.aigroupmobile.ui.components.SimpleLottie
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

@Composable
fun WelcomeGreetingPage(
  modifier: Modifier = Modifier,
  initial: WelcomeInitial,
  goNextWelcomePage: (WelcomeInitial) -> Unit = {},
) {
  val appName = stringResource(R.string.app_name).replace(" ", "")

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

        Column() {
          SimpleLottie(R.raw.emoji_sparkles)

          Spacer(Modifier.height(16.dp))

          Text(
            buildAnnotatedString {
              append(stringResource(R.string.label_welcome_greeting))
              append(" ")
              withStyle(SpanStyle(color = AppCustomTheme.colorScheme.tintColor)) {
                append(appName)
              }
            },
            fontWeight = FontWeight.SemiBold
          )
          Text(stringResource(R.string.label_welcome_init_app_desc), style = MaterialTheme.typography.titleMedium)

          Spacer(Modifier.height(16.dp))

          Button(
            onClick = {
              goNextWelcomePage(initial.copy(latestInitializedVersionCode = BuildConfig.VERSION_CODE))
            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = AppCustomTheme.colorScheme.primaryAction
            )
          ) {
            Text(stringResource(R.string.label_welcome_button_start))
          }
        }

        Spacer(Modifier.weight(1f))

        AppCopyRight()
        Spacer(Modifier.height(30.dp))
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun WelcomeGreetingPagePreview() {
  AIGroupAppTheme {
    WelcomeGreetingPage(
      initial = WelcomeInitial(
        hasUserProfile = false,
        hasFavoriteModel = false,
        emptySessionId = null,
        latestInitializedVersionCode = null
      )
    )
  }
}