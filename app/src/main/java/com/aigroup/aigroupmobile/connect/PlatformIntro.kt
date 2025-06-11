package com.aigroup.aigroupmobile.connect

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URL

sealed class PlatformLogo {
  data class Local(@DrawableRes val resId: Int) : PlatformLogo()
  data class Online(val url: String) : PlatformLogo()
}

fun PlatformLogo.coilDataModel(): Any {
  return when (this) {
    is PlatformLogo.Local -> resId
    is PlatformLogo.Online -> url
  }
}

interface PlatformIntro {
  val name: String
  val logo: PlatformLogo
  val usage: String

  @get:DrawableRes
  val icon: Int

  val siteLink: URL
}