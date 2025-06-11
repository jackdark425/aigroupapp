package com.aigroup.aigroupmobile.utils.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@ExperimentalPermissionsApi
@Composable
fun rememberPermissionStateSafe(permission: String, onPermissionResult: (Boolean) -> Unit = {}) = when {
  LocalInspectionMode.current -> remember {
    object : PermissionState {
      override val permission = permission
      override val status = PermissionStatus.Granted
      override fun launchPermissionRequest() = Unit
    }
  }
  else -> rememberPermissionState(permission, onPermissionResult)
}

//rememberPermissionStateSafe

@ExperimentalPermissionsApi
@Composable
fun rememberMultiplePermissionsStateSafe(permissions: List<String>, onPermissionResult: (Map<String, Boolean>) -> Unit = {}) = when {
  LocalInspectionMode.current -> remember {
    object : MultiplePermissionsState {
      override val allPermissionsGranted: Boolean = true
      override val permissions: List<PermissionState> = permissions.map { permission ->
        object : PermissionState {
          override val permission = permission
          override val status = PermissionStatus.Granted
          override fun launchPermissionRequest() = Unit
        }
      }
      override val revokedPermissions: List<PermissionState> = emptyList()
      override val shouldShowRationale: Boolean = false

      override fun launchMultiplePermissionRequest() = Unit
    }
  }
  else -> rememberMultiplePermissionsState(permissions, onPermissionResult)
}
