import com.google.protobuf.gradle.id
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.realm.kotlin)

  id("kotlin-kapt")
  id("com.google.dagger.hilt.android")
  id("com.google.protobuf") version "0.9.4"
  id("kotlinx-serialization")
  id("kotlin-parcelize")
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
  id("io.objectbox")
}

android {
  namespace = "com.aigroup.aigroupmobile"
  compileSdk = 34


  defaultConfig {
    applicationId = "com.aigroup.aigroupmobile"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }

    resourceConfigurations += listOf("zh-rCN", "zh-rHK", "en", "fr", "ja", "es")
//    buildTypes.getByName("debug") {
//      isPseudoLocalesEnabled = true
//    }
  }

  // 仅在CI环境(有CI环境变量或特定属性时)才配置签名
  val isCI = System.getenv("CI")?.toBoolean() ?: false
  if (isCI || project.hasProperty("KEYSTORE_PASSWORD")) {
    signingConfigs {
      create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreFile = rootProject.file("keystore.jks")
        
        if (!keystoreFile.exists()) {
          throw GradleException("Keystore file not found at: ${keystoreFile.absolutePath}")
        }

        if (keystorePropertiesFile.exists()) {
          val keystoreProperties = Properties()
          keystoreProperties.load(keystorePropertiesFile.inputStream())
          
          storeFile = keystoreFile
          storePassword = keystoreProperties["storePassword"] as String
          keyAlias = keystoreProperties["keyAlias"] as String
          keyPassword = keystoreProperties["keyPassword"] as String
        } else {
          // CI/CD环境下的配置
          val keystorePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
          val keyAlias = project.findProperty("KEY_ALIAS") as String?
          val keyPassword = project.findProperty("KEY_PASSWORD") as String?
          
          if (keystorePassword != null && keyAlias != null && keyPassword != null) {
            storeFile = keystoreFile
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
          } else {
            throw GradleException("Missing required signing properties in CI/CD environment")
          }
        }
      }
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
      isDebuggable = true
    }
    
    release {
      isMinifyEnabled = false
      isShrinkResources = false
      // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      
      // 仅在CI环境使用签名配置
      if (isCI || project.hasProperty("KEYSTORE_PASSWORD")) {
        signingConfig = signingConfigs.getByName("release")
      }

      ndk {
        abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.6"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      // TODO: do we need this?
      //  https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
//            excludes += "DebugProbesKt.bin"

      // fix tika
      excludes += "/META-INF/DEPENDENCIES"
      excludes += "/META-INF/LICENSE.md"
      excludes += "/META-INF/NOTICE.md"
      excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/spring.handlers"
      excludes += "/META-INF/spring.schemas"
      excludes += "/META-INF/cxf/bus-extensions.txt"
    }
  }

  androidResources {
    generateLocaleConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.recyclerview)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  implementation("androidx.compose.material3.adaptive:adaptive:1.0.0")
  implementation("androidx.compose.material3.adaptive:adaptive-layout:1.0.0")
  implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.0.0")

  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("org.robolectric:robolectric:4.13")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

  testImplementation(kotlin("test"))


  // logs
  implementation(libs.slf4j.android)
//  implementation("org.apache.logging.log4j:log4j-to-slf4j:2.23.1")

  // base library
  implementation(libs.kotlinx.datetime)
  implementation(libs.kaml) // yaml support
  implementation(libs.kotlin.xml.builder) // xml support

  // TODO: correct version
  implementation("com.github.jackdark425.openai-kotlin:openai-client:main")
  implementation(libs.generativeai)

  implementation(libs.ktor.client.android)
  implementation(libs.ktor.client.logging)
  implementation(libs.ktor.client.auth)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  testImplementation(libs.ktor.client.java)

  implementation(libs.langchain4j)

  // media
  implementation(libs.media3.exoplayer) // [Required] androidx.media3 ExoPlayer dependency
  implementation(libs.androidx.media3.session) // [Required] MediaSession Extension dependency
  implementation(libs.androidx.media3.ui) // [Required] Base Player UI

  // document parser
  implementation(libs.pdfbox.android) {
    // exclude "org.bouncycastle:bcprov-jdk15on"
    exclude(group = "org.bouncycastle")
  }
  implementation("org.apache.tika:tika-parsers:1.14") {
    // exclude protobuf
    exclude(group = "com.google.protobuf")

    // exclude problematic dependencies that contain desktop Java classes
    exclude(group = "org.apache.xmlbeans")
    exclude(group = "org.apache.cxf")
    exclude(group = "org.apache.sis")
    exclude(group = "org.springframework")
    exclude(group = "edu.ucar")
    exclude(group = "com.gemalto")
    exclude(group = "com.sun.msv")
    
    listOf("org.apache.commons", "commons-logging").forEach {
      exclude(group = it)
    }
  }
  implementation(libs.stax.api) // for Apache POI on Android
  implementation(libs.xmlbeans)


  // android architecture components
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.hilt.android)
  kapt(libs.hilt.android.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.runtime.livedata)
  implementation(libs.accompanist.permissions)

  implementation(libs.androidx.datastore)
  implementation(libs.protobuf.kotlin.lite)

  implementation(libs.jetpack.loading)

  // additional material components
  implementation(libs.material)

  // navigation related
  implementation(libs.androidx.navigation.compose)
  implementation(libs.bottombar)

  // ui like async image and icons
  implementation(libs.androidx.constraintlayout.compose)

  implementation(libs.coil.compose)
  implementation(libs.coil.video)
  implementation(libs.composeIcons.cssGg)
  implementation(libs.composeIcons.fontAwesome)

  implementation(libs.richtext.commonmark)
  implementation(libs.richtext.markdown)
  implementation(libs.richtext.ui.material3)

  implementation(libs.lottie.compose) // lottie animation

  // TODO: move in toml
  val emoji2_version = "1.5.0"
  implementation("androidx.emoji2:emoji2:$emoji2_version")
  implementation("androidx.emoji2:emoji2-views:$emoji2_version")
  implementation("androidx.emoji2:emoji2-emojipicker:$emoji2_version")
  implementation("androidx.emoji2:emoji2-views-helper:$emoji2_version")

  implementation("dev.chrisbanes.haze:haze:0.9.0-beta01") // blur related
  implementation("com.composables:materialcolors:1.0.0")
  implementation("com.jvziyaoyao.scale:image-viewer:1.1.0-alpha.2")
  implementation("io.sanghun:compose-video:1.2.0")

//    implementation("dev.snipme:highlights:0.9.2")
  implementation("dev.snipme:kodeview:0.8.0")

  implementation("com.github.lincollincol:compose-audiowaveform:1.1.1")

  implementation(libs.compose.cloudy)

  // gestures
  implementation(libs.swipe)

  // database
  implementation(libs.realm)
  implementation(libs.objectbox.kotlin)

  // sensory & vision & speech
  implementation(libs.client.sdk) // TODO: 改名
  implementation(libs.quickie.bundled) // qrcode
  implementation(libs.mlkit.barcode.scanning) // note qrcode is also use bundled model
}

protobuf {
  protoc {
    // a fix for m1
    artifact = if (osdetector.os == "osx") {
      "com.google.protobuf:protoc:3.21.11:osx-x86_64"
    } else {
      "com.google.protobuf:protoc:3.21.11"
    }
  }

  plugins { id("kotlin") }

  generateProtoTasks {
    all().forEach {
      it.builtins {
        create("java") {
          option("lite")
        }
//                create("kotlin") {
//                    option("lite")
//                }
      }
    }
  }
}

secrets {
  propertiesFileName = "secrets.properties"
  defaultPropertiesFileName = "secrets.defaults.properties"
  
  // 忽略缺失的配置文件
  ignoreList.add("keyToIgnore") // 忽略特定key

}