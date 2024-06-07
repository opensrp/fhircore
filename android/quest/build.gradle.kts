plugins {
  `fhir-properties`
  `fhir-jacoco-report`
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("de.mannodermaus.android-junit5")
  id("org.jetbrains.dokka")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.dagger.hilt.android")
  id("org.jetbrains.kotlin.android")
  id("com.google.firebase.firebase-perf")
  id("com.google.gms.google-services")
  id("com.google.firebase.crashlytics")
  id("com.google.firebase.appdistribution")
}

kotlin { jvmToolchain(17) }

android {
  namespace = "org.smartregister.fhircore.quest"
  compileSdk = Deps.sdk_versions.compile_sdk

  dataBinding { enable = true }

  defaultConfig {
    applicationId = "org.smartregister.fhircore"
    minSdk = Deps.sdk_versions.min_sdk
    targetSdk = Deps.sdk_versions.target_sdk
    versionCode = 4
    versionName = "0.0.4"
    multiDexEnabled = true

    buildConfigField("boolean", "SKIP_AUTH_CHECK", "false")
    buildConfigField("String", "FHIR_BASE_URL", """"${project.extra["FHIR_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_BASE_URL", """"${project.extra["OAUTH_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_CIENT_ID", """"${project.extra["OAUTH_CIENT_ID"]}"""")
    buildConfigField("String", "APP_ID", """"${project.extra["APP_ID"]}"""")
    buildConfigField(
      "String",
      "OAUTH_CLIENT_SECRET",
      """"${project.extra["OAUTH_CLIENT_SECRET"]}"""",
    )
    buildConfigField("String", "OAUTH_SCOPE", """"${project.extra["OAUTH_SCOPE"]}"""")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      enableV1Signing = false
      enableV2Signing = true

      keyAlias = System.getenv("KEYSTORE_ALIAS") ?: """${project.extra["KEYSTORE_ALIAS"]}"""
      keyPassword = System.getenv("KEY_PASSWORD") ?: """${project.extra["KEY_PASSWORD"]}"""
      storePassword =
        System.getenv("KEYSTORE_PASSWORD") ?: """${project.extra["KEYSTORE_PASSWORD"]}"""
      storeFile = file(System.getProperty("user.home") + "/fhircore.keystore.jks")
    }
  }

  buildTypes {
    debug { enableUnitTestCoverage = true }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      //            firebaseCrashlytics {
      //                isNativeSymbolUploadEnabled = false
      //            }
      firebaseAppDistribution {
        artifactType = "APK"
        releaseNotes = "Update"
      }
    }
  }
  packaging {
    resources {
      excludes +=
        listOf(
          "META-INF/ASL-2.0.txt",
          "META-INF/LGPL-3.0.txt",
          "license.html",
          "readme.html",
          "META-INF/DEPENDENCIES",
          "META-INF/LICENSE",
          "META-INF/LICENSE.txt",
          "META-INF/license.txt",
          "META-INF/license.html",
          "META-INF/LICENSE.md",
          "META-INF/NOTICE",
          "META-INF/NOTICE.txt",
          "META-INF/NOTICE.md",
          "META-INF/notice.txt",
          "META-INF/ASL2.0",
          "META-INF/ASL-2.0.txt",
          "META-INF/LGPL-3.0.txt",
          "META-INF/sun-jaxb.episode",
          "META-INF/*.kotlin_module",
          "META-INF/AL2.0",
          "META-INF/LGPL2.1",
        )
    }
  }

  compileOptions { isCoreLibraryDesugaringEnabled = true }

  kotlinOptions {
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
  }

  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true

    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  flavorDimensions += "apps"

  productFlavors {
    create("mwcore") {
      dimension = "apps"
      applicationIdSuffix = ".mwcore"
      versionNameSuffix = "-mwcore"
      versionCode = 1
      versionName = "0.0.1"
    }
    create("mwcoreDev") {
      dimension = "apps"
      applicationIdSuffix = ".mwcoreDev"
      versionNameSuffix = "-mwcoreDev"
      versionCode = 36
      versionName = "0.1.25"
    }
    create("mwcoreStaging") {
      dimension = "apps"
      applicationIdSuffix = ".mwcoreStaging"
      versionNameSuffix = "-mwcoreStaging"
      versionCode = 3
      versionName = "0.0.3"
    }
  }

  lint { abortOnError = false }

  testCoverage { jacocoVersion = Deps.versions.jacoco_tool }
}

configurations { all { exclude(group = "xpp3") } }

dependencies {
  coreLibraryDesugaring(Deps.desugar)
  implementation(project(":engine"))

  implementation("androidx.ui:ui-foundation:0.1.0-dev14")
  implementation(Deps.accompanist.swiperefresh)

  implementation("com.github.anrwatchdog:anrwatchdog:1.4.0")

  // Hilt - Dependency Injection
  implementation("com.google.dagger:hilt-android:${Deps.versions.hiltVersion}")
  kapt("com.google.dagger:hilt-compiler:${Deps.versions.hiltVersion}")

  testImplementation(Deps.junit5_api)
  testRuntimeOnly(Deps.junit5_engine)
  testRuntimeOnly(Deps.junit5_engine_vintage)
  testImplementation(Deps.robolectric)
  testImplementation(Deps.atsl.core)
  testImplementation(Deps.atsl.ext_junit)
  testImplementation(Deps.atsl.ext_junit_ktx)
  testImplementation(Deps.coroutines.test)
  testImplementation(Deps.androidx.core_test)
  debugImplementation(Deps.fragment_testing)
  releaseImplementation(Deps.fragment_testing)
  testImplementation(Deps.mockk)
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

  // analytics
  implementation(platform("com.google.firebase:firebase-bom:32.7.3"))

  implementation("com.google.firebase:firebase-perf-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-analytics-ktx")

  // Hilt test dependencies
  testImplementation("com.google.dagger:hilt-android-testing:${Deps.versions.hiltVersion}")
  kaptTest("com.google.dagger:hilt-android-compiler:${Deps.versions.hiltVersion}")

  androidTestImplementation(Deps.atsl.ext_junit)
  androidTestImplementation(Deps.atsl.espresso)
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  //     debugImplementation because LeakCanary should only run in debug builds.
  //    debugImplementation "com.squareup.leakcanary:leakcanary-android:2.7"
}

kapt { correctErrorTypes = true }

hilt { enableAggregatingTask = true }
