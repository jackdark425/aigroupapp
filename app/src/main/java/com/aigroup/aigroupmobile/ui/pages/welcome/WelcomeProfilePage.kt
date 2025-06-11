package com.aigroup.aigroupmobile.ui.pages.welcome

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.components.AppCopyRight
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import com.aigroup.aigroupmobile.viewmodels.WelcomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.reflect.KProperty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeProfilePage(
  modifier: Modifier = Modifier,
  goToAvatarPage: () -> Unit = {},
  viewModel: WelcomeViewModel = hiltViewModel()
) {
  val coroutineScope = rememberCoroutineScope()
  var userName by rememberSaveable { mutableStateOf("") }

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

        Column(Modifier.imePadding()) {
          Text("👋")

          Spacer(Modifier.height(16.dp))

          Text(
            stringResource(R.string.label_welcome_setup_username),
            fontWeight = FontWeight.SemiBold
          )
          Text(stringResource(R.string.label_welcome_setup_username_desc), style = MaterialTheme.typography.titleMedium)

          Spacer(Modifier.height(16.dp))

          BasicTextField(
            value = userName,
            onValueChange = {
              userName = it
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
            modifier = Modifier
              .height(40.dp)
              .clearFocusOnKeyboardDismiss()
              .imePadding(),
            singleLine = true,
          ) { innerTextField ->
            TextFieldDefaults.DecorationBox(
              value = userName,
              innerTextField = innerTextField,
              enabled = true,
              singleLine = true,
              placeholder = {
                Text(
                  text = stringResource(R.string.hint_welcome_placeholder_setup_username),
                  color = AppCustomTheme.colorScheme.secondaryLabel
                )
              },
              visualTransformation = VisualTransformation.None,
              interactionSource = remember { MutableInteractionSource() },
              contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                top = 0.dp,
                bottom = 0.dp
              ),
              shape = MaterialTheme.shapes.medium,
              colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = AppCustomTheme.colorScheme.primaryAction,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
              ),
            )
          }

          Spacer(Modifier.height(8.dp))

          Button(
            onClick = {
              coroutineScope.launch {
                viewModel.initializeUsername(userName)
                goToAvatarPage()
              }
            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = AppCustomTheme.colorScheme.primaryAction
            ),
            enabled = userName.isNotBlank()
          ) {
            Text(stringResource(R.string.label_welcome_button_continue))
          }
        }

        Spacer(Modifier.weight(1f))
      }
    }
  }
}
