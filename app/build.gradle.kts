import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.paparazzi)
}

// 비공개 키 로드: secrets.properties(미추적) 우선, 없으면 secrets.defaults.properties.
val secrets = Properties().apply {
    val real = rootProject.file("secrets.properties")
    val defaults = rootProject.file("secrets.defaults.properties")
    when {
        real.exists() -> real.inputStream().use { load(it) }
        defaults.exists() -> defaults.inputStream().use { load(it) }
    }
}
val naverMapClientId: String = secrets.getProperty("NAVER_MAP_CLIENT_ID", "")
val kakaoNativeAppKey: String = secrets.getProperty("KAKAO_NATIVE_APP_KEY", "")
val pickflowApiBaseUrl: String = secrets.getProperty("PICKFLOW_API_BASE_URL", "https://pickflow-api.us/api/")
val termsUrl: String = secrets.getProperty("TERMS_URL", "")
val privacyUrl: String = secrets.getProperty("PRIVACY_URL", "")
val noticeBoardMasterId: String = secrets.getProperty("NOTICE_BOARD_MASTER_ID", "1")
val kakaoRestApiKey: String = secrets.getProperty("KAKAO_REST_API_KEY", "")
val appleServiceId: String = secrets.getProperty("APPLE_SERVICE_ID", "")
val appleRedirectUri: String = secrets.getProperty("APPLE_REDIRECT_URI", "")

// release 서명: CD(GitHub Actions)에서 환경변수로 keystore를 주입할 때만 활성화.
// 로컬에서는 값이 없으므로 release 빌드도 debug 키로 서명되어 그대로 동작한다.
val releaseStoreFile: String? = System.getenv("KEYSTORE_FILE")
val releaseStorePassword: String? = System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("KEY_PASSWORD")
val hasReleaseSigning: Boolean =
    releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "com.pickflow.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pickflow.app"
        minSdk = 26
        targetSdk = 35
        // CD(GitHub Actions)에서 VERSION_CODE 주입, 로컬은 1.
        // release-aab 워크플로는 태그(vX.Y.Z)에서 X*10000+Y*100+Z 로 계산해 주입한다.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["naverMapClientId"] = naverMapClientId
        manifestPlaceholders["kakaoNativeAppKey"] = kakaoNativeAppKey
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        buildConfigField("String", "PICKFLOW_API_BASE_URL", "\"$pickflowApiBaseUrl\"")
        buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
        buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
        buildConfigField("long", "NOTICE_BOARD_MASTER_ID", "${noticeBoardMasterId}L")
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
        buildConfigField("String", "APPLE_SERVICE_ID", "\"$appleServiceId\"")
        buildConfigField("String", "APPLE_REDIRECT_URI", "\"$appleRedirectUri\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CD에서 keystore가 주입되면 release 키, 아니면 로컬용 debug 키로 서명.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.text.google.fonts)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.coil.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.naver.map)
    implementation(libs.kakao.user)
    implementation(libs.play.services.location)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    // Phase B — Compose UI test (Robolectric, JVM 실행)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
