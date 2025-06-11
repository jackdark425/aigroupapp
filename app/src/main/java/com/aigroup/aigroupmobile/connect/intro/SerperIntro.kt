package com.aigroup.aigroupmobile.connect.intro

import androidx.annotation.DrawableRes
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.PlatformIntro
import com.aigroup.aigroupmobile.connect.PlatformLogo
import java.net.URL

object SerperIntro: PlatformIntro {
  override val name: String = "Serper"
  override val logo = PlatformLogo.Local(R.drawable.ic_serper_logo)

  override val usage = appStringResource(R.string.label_service_usage_serper)

  @DrawableRes
  override val icon = R.drawable.ic_search_icon

  override val siteLink: URL = URL("https://serper.dev/playground")
}