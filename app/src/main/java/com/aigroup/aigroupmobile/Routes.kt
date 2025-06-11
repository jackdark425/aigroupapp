package com.aigroup.aigroupmobile

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.pages.SerperTokenPage
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.ui.pages.AboutPage
import com.aigroup.aigroupmobile.ui.pages.ChatDetailPage
import com.aigroup.aigroupmobile.ui.pages.ChatPage
import com.aigroup.aigroupmobile.ui.pages.SettingsPage
import com.aigroup.aigroupmobile.ui.pages.assistant.AssistantBuilderPage
import com.aigroup.aigroupmobile.ui.pages.assistant.AssistantDetailPage
import com.aigroup.aigroupmobile.ui.pages.assistant.AssistantStoreDetailPage
import com.aigroup.aigroupmobile.ui.pages.assistant.AssistantStorePage
import com.aigroup.aigroupmobile.ui.pages.buildHomeNavigation
import com.aigroup.aigroupmobile.ui.pages.chat.SessionSettingsPage
import com.aigroup.aigroupmobile.ui.pages.settings.ChatPropertiesSettingsPage
import com.aigroup.aigroupmobile.ui.pages.settings.CustomLLMModelEditPage
import com.aigroup.aigroupmobile.ui.pages.settings.CustomLLMModelListPage
import com.aigroup.aigroupmobile.ui.pages.settings.CustomLLMProviderEditPage
import com.aigroup.aigroupmobile.ui.pages.settings.CustomLLMProviderListPage
import com.aigroup.aigroupmobile.ui.pages.settings.ModelSelectPage
import com.aigroup.aigroupmobile.ui.pages.settings.ProfileSettingPage
import com.aigroup.aigroupmobile.ui.pages.settings.ChatTokenSettingPage
import com.aigroup.aigroupmobile.ui.pages.welcome.FirstIntroPage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeAvatarPage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeGreetingPage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeInitial
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeModelPage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeProfilePage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeRoot
import com.aigroup.aigroupmobile.viewmodels.CustomLLMProviderViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel

// TODO: 重构 Route: tree data structure?
// TODO: https://developer.android.com/guide/navigation/design?hl=zh-cn#compose using Object as route instead of string
sealed class Screen(val route: String, val name: String) {
  data object Welcome : Screen("welcome", "Welcome")
  data object Home : Screen("home", "首页")

  // welcomes
  data object WelcomeFirstIntro : Screen("first_intro", "First Intro") // logo animation page
  data object WelcomeGreeting : Screen("welcome_greeting", "Welcome Greeting") // sea hello to user
  data object WelcomeProfile : Screen("welcome_profile", "Welcome Profile") // user profile setting
  data object WelcomeAvatar : Screen("welcome_avatar", "Welcome Avatar") // user avatar setting
  data object WelcomeModel : Screen("welcome_model", "Welcome Model") // model select

  data object Settings : Screen("settings", "设置")

  data object UserProfileSetting : Screen("user_profile_setting", "个人资料")
  data object BotPropertiesSetting : Screen("bot_properties_setting", "聊天属性")
  data object ModelSelect : Screen("model_select", "模型选择")
  data object TokenSetting : Screen("token_setting", "Token 设置")
  data object About : Screen("about", "关于")
  
  // Custom LLM provider screens
  data object CustomLLMProviderList : Screen("custom_llm_provider_list", "自定义 LLM 提供商")
  data object CustomLLMProviderAdd : Screen("custom_llm_provider_add", "添加提供商")
  data class CustomLLMProviderEdit(val providerId: String) : Screen("custom_llm_provider_edit/$providerId", "编辑提供商") {
    companion object {
      const val route = "custom_llm_provider_edit/{providerId}"
    }
  }
  data class CustomLLMModelList(val providerId: String) : Screen("custom_llm_model_list/$providerId", "模型列表") {
    companion object {
      const val route = "custom_llm_model_list/{providerId}"
    }
  }
  data class CustomLLMModelAdd(val providerId: String) : Screen("custom_llm_model_add/$providerId", "添加模型") {
    companion object {
      const val route = "custom_llm_model_add/{providerId}"
    }
  }
  data class CustomLLMModelEdit(val providerId: String, val modelId: String) : Screen("custom_llm_model_edit/$providerId/$modelId", "编辑模型") {
    companion object {
      const val route = "custom_llm_model_edit/{providerId}/{modelId}"
    }
  }

  // assistant store and assistant
  data object AssistantStore : Screen("assistant_store", "Assistant Store")
  data class AssistantStoreDetail(val storeIdentifier: String) : Screen("assistant_store_detail/$storeIdentifier", "Assistant Store Detail") {
    companion object {
      const val route = "assistant_store_detail/{storeIdentifier}"
    }
  }
  data class AssistantDetail(val assistantId: String) : Screen("assistant_detail/$assistantId", "Assistant Detail") {
    companion object {
      const val route = "assistant_detail/{assistantId}"
    }
  }

  // third party platform settings TODO: 动态生成或者放到 connect package
  data object SerperSetting : Screen("serper_setting", "Serper 设置")

  data class ChatDetail(val chatId: String) : Screen("chat_detail/$chatId", "Chat Detail") {
    companion object {
      const val route = "chat_detail/{chatId}"
    }
  }

  data class SessionSettings(val chatId: String) : Screen("session_settings/$chatId", "Session Settings") {
    companion object {
      const val route = "session_settings/{chatId}"
    }
  }

  // Add AssistantBuilder route definition
  data object AssistantBuilder : Screen("assistant_builder", "Create Assistant")
}

// TODO: don't place all business logic here in route.

@Composable
fun RootNavigationGraph(
  initialWelcomeInitial: WelcomeInitial,
  navController: NavHostController = rememberNavController(),
) {
  var welcomeInitial by rememberSaveable {
    mutableStateOf(initialWelcomeInitial)
  }
  val defaultValueSessionId by remember {
    derivedStateOf {
      welcomeInitial.emptySessionId
    }
  }

  NavHost(
    navController = navController,
    startDestination = welcomeInitial.emptySessionId?.let { Screen.Home.route } ?: Screen.Welcome.route,
    enterTransition = {
      fadeIn(animationSpec = tween(300))
    },
    exitTransition = {
      fadeOut(animationSpec = tween(300))
    }
  ) {
    composable(route = Screen.Welcome.route) {
      val coroutineScope = rememberCoroutineScope()
      val haptic = LocalHapticFeedback.current

      WelcomeRoot(welcomeInitial) { welcomeController, nextWelcomeRoute, flushInitial ->
        fun welcomeNavigate(newInitial: WelcomeInitial) {
          welcomeInitial = newInitial
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)

          val screen = nextWelcomeRoute(newInitial)

          if (screen != null) {
            welcomeController.navigate(screen.route)
          } else {
            // flush welcome initial here
            coroutineScope.launch {
              flushInitial(newInitial)
              navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Welcome.route) {
                  inclusive = true
                }
              }
            }
          }
        }

        composable(route = Screen.WelcomeFirstIntro.route) {
          FirstIntroPage(
            goToWelcomePage = {
              welcomeNavigate(welcomeInitial)
            }
          )
        }

        composable(route = Screen.WelcomeGreeting.route) {
          WelcomeGreetingPage(
            initial = welcomeInitial,
            goNextWelcomePage = {
              welcomeNavigate(it)
            }
          )
        }

        composable(route = Screen.WelcomeProfile.route) {
          WelcomeProfilePage(
            goToAvatarPage = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              welcomeController.navigate(Screen.WelcomeAvatar.route)
            }
          )
        }

        composable(route = Screen.WelcomeAvatar.route) {
          WelcomeAvatarPage(
            initial = welcomeInitial,
            goNextWelcomePage = {
              welcomeNavigate(it)
            }
          )
        }

        composable(route = Screen.WelcomeModel.route) {
          WelcomeModelPage(
            initial = welcomeInitial,
            goNextWelcomePage = {
              welcomeNavigate(it)
            }
          )
        }
      }
    }

    buildHomeNavigation(
      drawerContent = { controller, state ->
        val navBackStackEntry by controller.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentTab = currentDestination?.hierarchy?.first()?.route

        val chatId = if (currentTab == Screen.ChatDetail.route) {
          navBackStackEntry?.arguments?.getString("chatId")
        } else {
          null
        }

        val coroutineScope = rememberCoroutineScope()
        fun closeDrawer() {
          coroutineScope.launch {
            state.adaptiveClose()
          }
        }

        ChatPage(
          chatId = chatId,
          onHideDrawer = {
            closeDrawer()
          },
          onNavigateAssistantBuilder = {
            closeDrawer()
            controller.navigate(Screen.AssistantBuilder.route)
          },
          onNavigateDetail = {
            if (it == chatId) {
              return@ChatPage
            }

            controller.navigate(Screen.ChatDetail(it).route) {
              popUpTo(controller.graph.findStartDestination().id) {
                inclusive = true
              }
              launchSingleTop = true
            }
            closeDrawer()
          },
          onNavigateDetailSetting = { chatId ->
            controller.navigate(Screen.SessionSettings(chatId).route)
            closeDrawer()
          },
          onNavigateAssistantSetting = { assistant ->
            controller.navigate(Screen.AssistantDetail(assistant.id.toHexString()).route)
            closeDrawer()
          },
          onGotoAssistantsStore = {
            controller.navigate(Screen.AssistantStore.route)
            closeDrawer()
          },
          splitMode = state.usingSplitLayout
        )
      },
    ) { homeNavController, state ->
      NavHost(
        route = Screen.Home.route,
        navController = homeNavController,
        // FIXME: why `chat_detail/` not work
        startDestination = Screen.ChatDetail.route,
//        startDestination = Screen.AssistantBuilder.route,
        enterTransition = {
          val isChatPage = targetState.destination.route == Screen.ChatDetail.route

          if (!isChatPage) {
            scaleIn(
              animationSpec = tween(220),
              initialScale = 1.1f
            ) + fadeIn(animationSpec = tween(220))
          } else {
            fadeIn(animationSpec = tween(0))
          }
        },
        exitTransition = {
          val isChatPage = targetState.destination.route == Screen.ChatDetail.route

          if (!isChatPage) {
            scaleOut(
              animationSpec = tween(
                durationMillis = 220,
              ), targetScale = 0.9f
            ) + fadeOut(tween())
          } else {
            fadeOut(animationSpec = tween(0))
          }
        }
      ) {
        composable(
          route = Screen.ChatDetail.route,
          arguments = listOf(navArgument("chatId") {
            type = NavType.StringType; defaultValue = defaultValueSessionId; nullable = true;
          })
        ) { backStackEntry ->
          val chatId = backStackEntry.arguments?.getString("chatId")
          val scope = rememberCoroutineScope()

          ChatDetailPage(
            onOpenDrawer = {
              scope.launch { state.adaptiveToggle() }
            },
            onNavigateDetail = {
              homeNavController.navigate(Screen.ChatDetail(it).route)
            },
            onOpenUserProfile = {
              homeNavController.navigate(Screen.Settings.route)
            },
            onOpenSetting = {
              chatId?.let {
                homeNavController.navigate(Screen.SessionSettings(it).route)
              }
            },
            onNavigateToAssistantStore = {
              homeNavController.navigate(Screen.AssistantStore.route)
            },
            splitMode = state.usingSplitLayout
          )
        }

        composable(route = Screen.Settings.route) {
          SettingsPage(
            onOpenPage = {
              homeNavController.navigate(it.route)
            },
            onBack = {
              homeNavController.popBackStack()
            }
          )
        }

        composable(
          route = Screen.SessionSettings.route,
          arguments = listOf(navArgument("chatId") {
            type = NavType.StringType; defaultValue = defaultValueSessionId; nullable = true;
          })
        ) {
          SessionSettingsPage(
            onBack = {
              homeNavController.popBackStack()
            }
          )
        }

        // assistant store and assistant
        composable(route = Screen.AssistantStore.route) {
          AssistantStorePage(
            onBack = { homeNavController.popBackStack() },
            onGotoDetail = { assistant ->
              homeNavController.navigate(
                Screen.AssistantStoreDetail(assistant.identifier).route
              )
            }
          )
        }
        composable(
          route = Screen.AssistantStoreDetail.route,
          arguments = listOf(navArgument("storeIdentifier") {
            type = NavType.StringType
          })
        ) { backStackEntry ->
          val storeIdentifier = backStackEntry.arguments?.getString("storeIdentifier")
          storeIdentifier?.let {
            AssistantStoreDetailPage(
              storeIdentifier = it,
              onBack = { homeNavController.popBackStack() },
              onNavigateToSession = { session ->
                homeNavController.navigate(
                  Screen.ChatDetail(session.id.toHexString()).route
                )
              }
            )
          }
        }
        composable(
          route = Screen.AssistantDetail.route,
          arguments = listOf(navArgument("assistantId") {
            type = NavType.StringType
          })
        ) {
          AssistantDetailPage(
            onBack = { homeNavController.popBackStack() },
            onNavigateToSession = { session ->
              homeNavController.navigate(
                Screen.ChatDetail(session.id.toHexString()).route
              ) {
                popUpTo(Screen.Home.route) {
                  inclusive = true
                }
              }
            }
          )
        }

        composable(route = Screen.ModelSelect.route) {
          ModelSelectPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
        composable(route = Screen.BotPropertiesSetting.route) {
          ChatPropertiesSettingsPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
        composable(route = Screen.UserProfileSetting.route) {
          ProfileSettingPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
        composable(route = Screen.TokenSetting.route) {
          ChatTokenSettingPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
        composable(route = Screen.About.route) {
          AboutPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
        
        // Custom LLM provider screens
        composable(route = Screen.CustomLLMProviderList.route) {
          CustomLLMProviderListPage(
            onBack = { homeNavController.popBackStack() },
            onNavigateToAddProvider = { 
              homeNavController.navigate(Screen.CustomLLMProviderAdd.route)
            },
            onNavigateToEditProvider = { provider ->
              homeNavController.navigate(Screen.CustomLLMProviderEdit(provider.provider.id).route)
            }
          )
        }
        
        composable(route = Screen.CustomLLMProviderAdd.route) {
          CustomLLMProviderEditPage(
            onBack = { homeNavController.popBackStack() },
            provider = null,
            onNavigateToAddModel = { provider ->
              homeNavController.navigate(Screen.CustomLLMModelAdd(provider.provider.id).route)
            },
            onNavigateToEditModel = { provider, model ->
              homeNavController.navigate(Screen.CustomLLMModelEdit(provider.provider.id, model.id).route)
            }
          )
        }
        
        composable(
          route = Screen.CustomLLMProviderEdit.route,
          arguments = listOf(navArgument("providerId") {
            type = NavType.StringType
          })
        ) { backStackEntry ->
          val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
          val viewModel: CustomLLMProviderViewModel = hiltViewModel()
          var provider by remember { mutableStateOf<CustomChatServiceProvider?>(null) }
          
          LaunchedEffect(providerId) {
            Log.d("CustomLLMProviderEdit", "Fetching provider with id: $providerId")
            provider = viewModel.getProviderById(providerId)
            Log.d("CustomLLMProviderEdit", "Provider fetched: ${provider != null}")
          }
          
          provider?.let {
            CustomLLMProviderEditPage(
              onBack = { homeNavController.popBackStack() },
              provider = it,
              onNavigateToAddModel = { provider ->
                homeNavController.navigate(Screen.CustomLLMModelAdd(provider.provider.id).route)
              },
              onNavigateToEditModel = { provider, model ->
                homeNavController.navigate(Screen.CustomLLMModelEdit(provider.provider.id, model.id).route)
              }
            )
          } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        
        composable(
          route = Screen.CustomLLMModelList.route,
          arguments = listOf(navArgument("providerId") {
            type = NavType.StringType
          })
        ) { backStackEntry ->
          val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
          val viewModel: CustomLLMProviderViewModel = hiltViewModel()
          var provider by remember { mutableStateOf<CustomChatServiceProvider?>(null) }
          
          LaunchedEffect(providerId) {
            Log.d("CustomLLMModelList", "Fetching provider with id: $providerId")
            provider = viewModel.getProviderById(providerId)
            Log.d("CustomLLMModelList", "Provider fetched: ${provider != null}")
          }
          
          provider?.let {
            CustomLLMModelListPage(
              onBack = { homeNavController.popBackStack() },
              provider = it,
              onNavigateToAddModel = { provider ->
                homeNavController.navigate(Screen.CustomLLMModelAdd(provider.provider.id).route)
              },
              onNavigateToEditModel = { provider, model ->
                homeNavController.navigate(Screen.CustomLLMModelEdit(provider.provider.id, model.id).route)
              }
            )
          } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        
        composable(
          route = Screen.CustomLLMModelAdd.route,
          arguments = listOf(navArgument("providerId") {
            type = NavType.StringType
          })
        ) { backStackEntry ->
          val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
          val viewModel: CustomLLMProviderViewModel = hiltViewModel()
          var provider by remember { mutableStateOf<CustomChatServiceProvider?>(null) }
          
          LaunchedEffect(providerId) {
            Log.d("CustomLLMModelAdd", "Fetching provider with id: $providerId")
            provider = viewModel.getProviderById(providerId)
            Log.d("CustomLLMModelAdd", "Provider fetched: ${provider != null}")
          }
          
          provider?.let {
            CustomLLMModelEditPage(
              onBack = { homeNavController.popBackStack() },
              provider = it,
              model = null
            )
          } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        
        composable(
          route = Screen.CustomLLMModelEdit.route,
          arguments = listOf(
            navArgument("providerId") { type = NavType.StringType },
            navArgument("modelId") { type = NavType.StringType }
          )
        ) { backStackEntry ->
          val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
          val modelId = backStackEntry.arguments?.getString("modelId") ?: ""
          val viewModel: CustomLLMProviderViewModel = hiltViewModel()
          var provider by remember { mutableStateOf<CustomChatServiceProvider?>(null) }
          var model by remember { mutableStateOf<CustomLLMModel?>(null) }
          
          LaunchedEffect(providerId, modelId) {
            Log.d("CustomLLMModelEdit", "Fetching provider with id: $providerId")
            provider = viewModel.getProviderById(providerId)
            Log.d("CustomLLMModelEdit", "Provider fetched: ${provider != null}")
            
            if (provider != null) {
              Log.d("CustomLLMModelEdit", "Fetching models for provider id: ${provider!!.provider.id}")
              val models = viewModel.getModelsForProvider(provider!!.provider.providerId)
              Log.d("CustomLLMModelEdit", "Models fetched: ${models.size}")
              
              model = models.find { modelItem -> modelItem.id == modelId }
              Log.d("CustomLLMModelEdit", "Model found: ${model != null}")
            }
          }
          
          if (provider != null && model != null) {
            val safeProvider = provider!!
            val safeModel = model
            CustomLLMModelEditPage(
              onBack = { homeNavController.popBackStack() },
              provider = safeProvider,
              model = safeModel
            )
          } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        }

        // third party platform settings
        composable(route = Screen.SerperSetting.route) {
          SerperTokenPage(
            onBack = { homeNavController.popBackStack() }
          )
        }

        // Add AssistantBuilder route configuration 
        composable(route = Screen.AssistantBuilder.route) {
          AssistantBuilderPage(
            onBack = { homeNavController.popBackStack() }
          )
        }
      }
    }

  }
}
