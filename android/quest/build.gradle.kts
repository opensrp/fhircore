plugins {
  `fhir-properties`
  `fhir-jacoco-report`
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("de.mannodermaus.android-junit5") version "1.9.3.0"
  id("org.jetbrains.dokka")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("dagger.hilt.android.plugin")
  id("org.jetbrains.kotlin.android")
  id("com.google.firebase.firebase-perf")
  id("com.google.gms.google-services")
  id("com.google.firebase.crashlytics")
}

kotlin { jvmToolchain(17) }

android {
  namespace = "org.smartregister.fhircore.quest"
  compileSdk = Deps.sdk_versions.compile_sdk

  dataBinding { enable = true }

  defaultConfig {
    applicationId = "org.smartregister.fhircore"
    minSdk = Deps.sdk_versions.min_sdk
    versionCode = 4
    versionName = "0.0.4"
    multiDexEnabled = true

    buildConfigField("boolean", "SKIP_AUTH_CHECK", "false")
    buildConfigField("String", "FHIR_BASE_URL", """"${project.extra["FHIR_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_BASE_URL", """"${project.extra["OAUTH_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_CIENT_ID", """"${project.extra["OAUTH_CIENT_ID"]}"""")
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

  composeOptions { kotlinCompilerExtensionVersion = "1.4.8" }

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
    create("quest") {
      dimension = "apps"
      applicationIdSuffix = ".quest"
      versionNameSuffix = "-quest"
      isDefault = true
    }
    create("ecbis") {
      dimension = "apps"
      applicationIdSuffix = ".ecbis"
      versionNameSuffix = "-ecbis"
      versionCode = 3
      versionName = "0.0.6"
    }
    create("g6pd") {
      dimension = "apps"
      applicationIdSuffix = ".g6pd"
      versionNameSuffix = "-g6pd"
    }
    create("mwcore") {
      dimension = "apps"
      applicationIdSuffix = ".mwcore"
      versionNameSuffix = "-mwcore"
      versionCode = 36
      versionName = "0.1.25-alpha01"
    }
    create("mwcoreDev") {
      dimension = "apps"
      applicationIdSuffix = ".mwcoreDev"
      versionNameSuffix = "-mwcoreDev"
      versionCode = 33
      versionName = "0.1.22"
    }
    create("mwcoreProd") {
      dimension = "apps"
      applicationIdSuffix = ".mwcoreProd"
      versionNameSuffix = "-mwcoreProd"
      versionCode = 15
      versionName = "0.1.4"
    }
    create("afyayangu") {
      dimension = "apps"
      applicationIdSuffix = ".afyayangu"
      versionNameSuffix = "-afyayangu"
      versionCode = 1
      versionName = "0.0.1"
    }
  }

  lint { abortOnError = false }

  configurations.configureEach {
    resolutionStrategy { force("ca.uhn.hapi.fhir:org.hl7.fhir.utilities:5.5.7") }
  }

  testCoverage { jacocoVersion = Deps.versions.jacoco_tool }
}

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
  implementation(platform("com.google.firebase:firebase-bom:31.2.0"))

  implementation("com.google.firebase:firebase-perf-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-analytics-ktx")

  // Hilt test dependencies
  testImplementation("com.google.dagger:hilt-android-testing:${Deps.versions.hiltVersion}")
  kaptTest("com.google.dagger:hilt-android-compiler:${Deps.versions.hiltVersion}")

  androidTestImplementation(Deps.atsl.ext_junit)
  androidTestImplementation(Deps.atsl.espresso)
  val composeVersion = Deps.versions.compose
  debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
  testImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
  //     debugImplementation because LeakCanary should only run in debug builds.
  //    debugImplementation "com.squareup.leakcanary:leakcanary-android:2.7"

  testImplementation("info.cqframework:cql-to-elm:1.5.6")
}

kapt { correctErrorTypes = true }

hilt { enableAggregatingTask = true }
