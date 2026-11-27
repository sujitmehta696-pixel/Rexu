plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.aurumtrade.shared"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(platform(libs.firebase.bom))
  api(libs.androidx.compose.material.icons.core)
  api(libs.androidx.compose.material.icons.extended)
  api(libs.androidx.compose.material3)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.ui.graphics)
  api(libs.androidx.compose.ui.tooling.preview)
  api(libs.androidx.core.ktx)
  api(libs.androidx.lifecycle.runtime.compose)
  api(libs.androidx.lifecycle.runtime.ktx)
  api(libs.androidx.lifecycle.viewmodel.compose)
  api(libs.firebase.firestore)
  api(libs.firebase.auth)
  api(libs.androidx.credentials)
  api(libs.androidx.credentials.play.services)
  api(libs.googleid)
  api(libs.kotlinx.coroutines.android)
  api(libs.kotlinx.coroutines.core)
}
