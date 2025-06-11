@file:OptIn(ExperimentalLayoutApi::class, ExperimentalLayoutApi::class)

package com.aigroup.aigroupmobile.ui.pages.welcome

import android.os.Parcelable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.aigroup.aigroupmobile.Screen
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.viewmodels.WelcomeViewModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WelcomeInitial(
  val hasFavoriteModel: Boolean,
  val hasUserProfile: Boolean,

  val emptySessionId: String?,

  /**
   * 用户最新初始化的版本号
   */
  val latestInitializedVersionCode: Int?
): Parcelable

@Composable
fun WelcomeRoot(
  initial: WelcomeInitial,
  viewModel: WelcomeViewModel = hiltViewModel(),
  builder: NavGraphBuilder.(
    controller: NavHostController,
    nextRoute: (WelcomeInitial) -> Screen?,
    flushInitial: suspend (WelcomeInitial) -> Unit
  ) -> Unit = { _, _, _ -> },
) {
  val controller = rememberNavController()

  var alreadyShownLogoAnimation by remember { mutableStateOf(false) }

  fun nextWelcomeRoute(@Suppress("UNUSED_PARAMETER") initial: WelcomeInitial): Screen? {
    // 跳过所有欢迎页面，直接进入主界面
    return null
  }

  val initialWelcomeRoute = remember { nextWelcomeRoute(initial) }

  LaunchedEffect(Unit) {
    if (initialWelcomeRoute == Screen.WelcomeFirstIntro) {
      alreadyShownLogoAnimation = true
    }
  }

  NavHost(
    route = Screen.Welcome.route,
    navController = controller,
    startDestination = initialWelcomeRoute?.route ?: Screen.WelcomeGreeting.route,
    enterTransition = { fadeIn(animationSpec = tween(700)) },
    exitTransition = { fadeOut(animationSpec = tween(300)) },
    builder = {
      builder(
        controller,
        { nextWelcomeRoute(it) },
        viewModel::flushWelcomeInitial
      )
    }
  )
}