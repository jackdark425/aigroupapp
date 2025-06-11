package com.aigroup.aigroupmobile.ui.pages.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.DarkToggleButton
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.SettingItemSelection
import com.aigroup.aigroupmobile.services.rememberQRCodeScanner
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Rename
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.ScannerConfig
import kotlinx.coroutines.launch

private const val TAG = "SettingPageAppBar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun SettingPageAppBar(
  userProfile: UserProfile,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: () -> Unit = {},
  onUpdateToken: (String?) -> Unit = {},
) {
  val coroutineScope = rememberCoroutineScope()

  val alpha = scrollBehavior.state.overlappedFraction
  val context = LocalContext.current

  fun updateToken(token: String?) {
    if (token.isNullOrEmpty()) {
      Toast.makeText(context, context.getString(R.string.toast_token_cannot_empty), Toast.LENGTH_SHORT).show()
      return
    }
    onUpdateToken(token)
  }

  var showTokenDropdown by remember { mutableStateOf(false) }
  val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanCustomCode()) { result ->
    // handle QRResult
    if (result is QRResult.QRSuccess) {
      val content = result.content.rawValue
      updateToken(content)
    }
  }

  var showTokenModal by remember { mutableStateOf(false) }
  var qrCodeScanner = rememberQRCodeScanner()

  CenterAlignedTopAppBar(
    scrollBehavior = scrollBehavior,
    title = {
      Text(userProfile.username, modifier = Modifier.alpha(alpha))
    },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Transparent,
      scrolledContainerColor = Color.Transparent,
    ),
    actions = {
      Box {
        IconButton(onClick = {
          showTokenDropdown = true
        }) {
          Icon(painterResource(R.drawable.ic_secret_files_fill_icon), "", Modifier.size(20.dp))
          // TODO: show popup here
        }

        DropdownMenu(
          expanded = showTokenDropdown,
          onDismissRequest = {
            showTokenDropdown = false
          },
          shape = MaterialTheme.shapes.medium,
          containerColor = MaterialTheme.colorScheme.background
        ) {
          AppDropdownMenuItem(
            onClick = {
              showTokenDropdown = false
              showTokenModal = true
            },
            text = { Text(stringResource(R.string.label_menu_input_app_token)) },
            leadingIcon = { Icon(CssGgIcons.Rename, "", Modifier.size(15.dp)) }
          )
          AppDropdownMenuItem(
            onClick = {
              showTokenDropdown = false
              scanQrCodeLauncher.launch(
                ScannerConfig.build {
                  setOverlayStringRes(R.string.scan_qr_code_overlay)
                  setShowTorchToggle(true) // show or hide (default) torch/flashlight toggle button
                  setShowCloseButton(true) // show or hide (default) close button
                  setHapticSuccessFeedback(true) // provide haptic feedback on successful scan
                }
              )
            },
            text = { Text(stringResource(R.string.label_menu_qrcode_app_token)) },
            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_qr_scan_icon), "", Modifier.size(15.dp)) }
          )
          MediaSelector.TakeGalleryButton(
            type = MediaSelector.MediaType.Image,
            onMediaSelected = {
              showTokenDropdown = false

              if (it == null) {
                return@TakeGalleryButton
              }

              coroutineScope.launch {
                val result = qrCodeScanner.scanQRCode(it.uri)
                updateToken(result)
              }
            }
          ) {
            AppDropdownMenuItem(
              onClick = {
                this.onClick()
              },
              text = { Text(stringResource(R.string.label_menu_qrcode_gallery_app_token)) },
              leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_qr_code_icon), "", Modifier.size(15.dp)) }
            )
          }
        }
      }
      DarkToggleButton()
    }
  )

  if (showTokenModal) {
    SettingItemSelection(
      onDismiss = { showTokenModal = false },
    ) {
      createInput(
        stringResource(R.string.hint_input_app_token),
        hideOnSubmit = true,
        autoFocus = true,
        showConfirmButton = true,
        secret = true
      ) {
        updateToken(it)
      }
    }
  }
}