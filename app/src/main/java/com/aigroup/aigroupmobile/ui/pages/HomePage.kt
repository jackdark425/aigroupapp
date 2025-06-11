package com.aigroup.aigroupmobile.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aigroup.aigroupmobile.Screen
import com.aigroup.aigroupmobile.ui.components.AdaptiveDrawer
import com.aigroup.aigroupmobile.ui.components.AdaptiveDrawerState
import com.aigroup.aigroupmobile.ui.components.rememberAdaptiveDrawerState

fun NavGraphBuilder.buildHomeNavigation(
  drawerContent: @Composable (homeNavController: NavHostController, state: AdaptiveDrawerState) -> Unit,
  content: @Composable (homeNavController: NavHostController, state: AdaptiveDrawerState) -> Unit,
) {
  composable(route = Screen.Home.route) {
    val navigationController = rememberNavController()
    val drawerState = rememberAdaptiveDrawerState()

    val navBackStackEntry by navigationController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.hierarchy?.first()?.route

    val isHome = currentRoute == Screen.ChatDetail.route

    AdaptiveDrawer(
      state = drawerState,
      gesturesEnabled = isHome,
      drawerContent = {
        drawerContent(navigationController, drawerState)
      },
    ) {
      Box(Modifier.background(MaterialTheme.colorScheme.background)) {
        content(navigationController, drawerState)
      }
    }

  }
}
