package com.aigroup.aigroupmobile.connect.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.PlatformIntro
import com.aigroup.aigroupmobile.connect.coilDataModel
import com.aigroup.aigroupmobile.connect.intro.SerperIntro
import com.aigroup.aigroupmobile.ui.components.LittleSwitch
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenSetupPage(
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
  intro: PlatformIntro,
  onTokenUpdate: (String) -> Unit = {},
  enable: Boolean = true,
  onDisable: () -> Unit = {},
) {
  val uriHandler = LocalUriHandler.current

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.label_service_setup, intro.name), fontWeight = FontWeight.SemiBold) },
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
      Column(
        Modifier.padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
              intro.logo.coilDataModel(),
              placeholder = if (LocalInspectionMode.current) painterResource(R.drawable.ic_serper_logo) else null,
              contentDescription = null,
              modifier = Modifier.width(150.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
              intro.usage,
              style = MaterialTheme.typography.bodySmall,
              color = AppCustomTheme.colorScheme.secondaryLabel,
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        SectionListSection(
          sectionHeader = stringResource(R.string.label_service_setup_basic_info),
          showTitle = false,
        ) {
          AnimatedContent(enable) {
            if (it) {
              SectionListItem(
                title = stringResource(R.string.label_settings_item_already_set),
                icon = ImageVector.vectorResource(intro.icon),
                noIconBg = true,
                trailingContent = {
                  LittleSwitch(
                    checked = enable,
                    onCheckedChange = {
                      if (!it) {
                        onDisable()
                      }
                    },
                    modifier = Modifier,
                  )
                }
              )
            } else {
              SectionListItem(
                title = stringResource(R.string.label_service_setup_set_token),
                icon = CssGgIcons.Lock,
                noIconBg = true,
                trailingDetailContent = {
                  Text(stringResource(R.string.label_settings_item_not_set))
                },
                modalContent = {
                  createInput(
                    label = stringResource(R.string.hint_service_setup_token, intro.name),
                    secret = true,
                    hideOnSubmit = true,
                    autoFocus = true,
                    showConfirmButton = true,
                    onSubmit = {
                      onTokenUpdate(it)
                    }
                  )
                }
              )
            }
          }
        }

        Spacer(Modifier.weight(1f))

        TextButton(
          onClick = {
            uriHandler.openUri(intro.siteLink.toString())
          },
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialColors.Blue[600]
          )
        ) {
          Text(stringResource(R.string.label_button_service_setup_official_site, intro.name))
        }
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun TokenSetupPagePreview() {
  var token: String? by remember { mutableStateOf(null) }
  val enable by remember {
    derivedStateOf { token != null }
  }

  AIGroupAppTheme {
    TokenSetupPage(
      intro = SerperIntro,
      enable = enable,
      onDisable = { token = null },
      onTokenUpdate = { token = it }
    )
  }
}