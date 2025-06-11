package com.aigroup.aigroupmobile.ui.pages

import androidx.compose.runtime.Composable
import com.aigroup.aigroupmobile.RootNavigationGraph
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeInitial

@Composable
fun RootPage(welcomeInitial: WelcomeInitial) {
  RootNavigationGraph(
    initialWelcomeInitial = welcomeInitial,
  )
}

