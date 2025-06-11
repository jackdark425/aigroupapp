package com.aigroup.aigroupmobile

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.ui.pages.RootPage
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeInitial
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.services.TextSpeaker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


val LocalPathManager = staticCompositionLocalOf<PathManager> {
  error("No PathManager provided")
}

val LocalTextSpeaker = staticCompositionLocalOf<TextSpeaker> {
  error("No TextSpeaker provided")
}

@AndroidEntryPoint
// original is ComponentActivity
class MainActivity : AppCompatActivity() {

  companion object {
    const val TAG = "MainActivity"
  }

  @Inject
  lateinit var pathManger: PathManager

  @Inject
  lateinit var dataStore: DataStore<AppPreferences>

  @Inject
  lateinit var conversationDao: ChatConversationDao

  @Inject
  lateinit var userDao: UserDao


  private fun isTabletDevice(configuration: Configuration = resources.configuration): Boolean {
    val screenLayoutSize = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    val validSize = screenLayoutSize >= Configuration.SCREENLAYOUT_SIZE_LARGE

    return validSize
  }

  private suspend fun getEmptySession(): ChatSession? {
    val preferences = dataStore.data.first()
    return if (preferences.defaultModelCode.isNullOrEmpty()) {
      null
    } else {
      conversationDao.ensureEmptySession(
        ModelCode.fromFullCode(preferences.defaultModelCode)
      )
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    Log.i(TAG, "onConfigurationChanged, isTablet: ${isTabletDevice(newConfig)} (${newConfig.orientation})")
    if (!isTabletDevice(newConfig)) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    // note that we already set android:configChanges="screenLayout|smallestScreenSize|screenSize|orientation"

    super.onCreate(savedInstanceState)


    Log.i(TAG, "onCreate, isTablet: ${isTabletDevice()} (${resources.configuration.orientation})")
    if (!isTabletDevice()) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // get resources here because when os <= android 12, locales get in application always
    // return system config instead of app config
    MainApplication.resources = resources

    enableEdgeToEdge()
    val appPreferences = runBlocking { dataStore.data.first() }

    setContent {
      val liveUiMode by dataStore.data.map { it.uiMode }.collectAsStateWithLifecycle(
        appPreferences.uiMode
      )
      val liveColorScheme by dataStore.data.map { it.colorScheme }.collectAsStateWithLifecycle(
        appPreferences.colorScheme
      )
      val liveDynamicColor by dataStore.data.map { it.usingAndroidDynamicColor }.collectAsStateWithLifecycle(
        appPreferences.usingAndroidDynamicColor
      )
      var welcomeInitial: WelcomeInitial? by rememberSaveable { mutableStateOf(null) }

      LaunchedEffect(Unit) {
        // 自动创建用户名为"jack"的用户（如果不存在）
        val hasLocalUser = userDao.hasLocalUser()
        if (!hasLocalUser) {
          userDao.createInitialLocalUser("jack")
        }

        // 设置默认模型为deepseek:deepseek-chat（如果未设置）
        if (appPreferences.defaultModelCode.isNullOrEmpty()) {
          dataStore.updateData { preferences ->
            preferences.toBuilder()
              .setDefaultModelCode("deepseek:deepseek-chat")
              .build()
          }
        }

        // 设置默认颜色方案为经典配色
        if (appPreferences.colorScheme == AppPreferences.ColorScheme.UNRECOGNIZED) {
          Log.i("MainActivity", "配色方案未识别，设置为经典配色")
          dataStore.updateData { preferences ->
            preferences.toBuilder()
              .setColorScheme(AppPreferences.ColorScheme.CLASSIC)
              .build()
          }
        } else {
          Log.i("MainActivity", "当前配色方案: ${appPreferences.colorScheme}")
        }

        // 默认禁用动态颜色，使用自定义颜色方案
        if (appPreferences.usingAndroidDynamicColor) {
          dataStore.updateData { preferences ->
            preferences.toBuilder()
              .setUsingAndroidDynamicColor(false)
              .build()
          }
        }

        val emptySession = getEmptySession()

        // TODO: 未来添加一个 Migration MAP 用来做版本升级
        welcomeInitial = WelcomeInitial(
          hasFavoriteModel = true, // 强制设为true，跳过模型选择
          hasUserProfile = true,   // 强制设为true，跳过用户资料设置
          emptySessionId = emptySession?.id?.toHexString(),
          latestInitializedVersionCode = 1 // 设置为已初始化，跳过欢迎页面
        )
      }

      AIGroupAppTheme(
        uiModePreferences = liveUiMode,
        colorScheme = liveColorScheme,
        dynamicColor = liveDynamicColor,
      ) {
        val uiMode by LocalUiMode.current
        val useDark = uiMode == UiMode.Dark

        DisposableEffect(useDark) {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
              android.graphics.Color.TRANSPARENT,
              android.graphics.Color.TRANSPARENT,
            ) { useDark },
          )
          onDispose {}
        }

        CompositionLocalProvider(
          LocalPathManager provides pathManger,
          LocalTextSpeaker provides TextSpeaker(this)
        ) {
          welcomeInitial?.let {
            RootPage(welcomeInitial = it)
          }
        }
      }
    }
  }

}
