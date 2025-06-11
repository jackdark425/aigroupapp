package com.aigroup.aigroupmobile.utils.system

import android.os.Build

object PermissionUtils {

  val visualMediaPermission: List<String>
    get() {
      when (Build.VERSION.SDK_INT) {
        // android 14 +
        in Build.VERSION_CODES.UPSIDE_DOWN_CAKE..Int.MAX_VALUE -> {
          return listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
          )
        }

        // android 13
        Build.VERSION_CODES.TIRAMISU -> {
          return listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
          )
        }

        // android 13 lower
        else -> {
          return listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
          )
        }
      }
    }

}