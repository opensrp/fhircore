import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
  apply(from = "../jacoco.gradle.kts")
  apply(from = "../properties.gradle.kts")
}

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
  id("kotlin-parcelize")
  id("de.mannodermaus.android-junit5")
  id("dagger.hilt.android.plugin")
  id("androidx.navigation.safeargs")
  id("org.jetbrains.kotlin.plugin.serialization")
}

android {
  compileSdk = 33

  defaultConfig {
    minSdk = 26
    targetSdk = 33
    buildConfigField("String", "MAPBOX_SDK_TOKEN", """"${project.extra["MAPBOX_SDK_TOKEN"]}"""")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("debug") { isTestCoverageEnabled = true }

    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
  }

  buildFeatures {
    compose = true
    viewBinding = true
  }

  composeOptions { kotlinCompilerExtensionVersion = "1.3.0" }

  packagingOptions {
    resources.excludes.addAll(
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
        "META-INF/INDEX.LIST"
      )
    )
  }

  testOptions {
    animationsDisabled = true
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  testCoverage { jacocoVersion = "0.8.7" }
}

tasks.withType<Test> {
  testLogging { events = setOf(TestLogEvent.SKIPPED) }
  jvmArgs("-Xms4608m -Xmx4608m -ea -noverify")
}

configurations { all { exclude(group = "xpp3") } }

dependencies {
  coreLibraryDesugaring(libs.core.desugar)

  implementation(project(":engine"))
  implementation(libs.core.ktx)
  implementation(libs.appcompat)
  implementation(libs.material)
  implementation(libs.dagger.hilt.android)
  implementation(libs.hilt.work)
  implementation(libs.mapbox.sdk.turf)
  implementation(libs.kujaku.library) {
    isTransitive = true
    exclude(group = "stax", module = "stax-api")
  }

  // Annotation processors
  kapt(libs.hilt.compiler)
  kapt(libs.dagger.hilt.compiler)

  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.vintage.engine)

  // Unit test dependencies
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.robolectric)
  testImplementation(libs.junit)
  testImplementation(libs.junit.ktx)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.core.testing)
  testImplementation(libs.mockk)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.hilt.android.testing)
  testImplementation(libs.navigation.testing)

  // To run only on debug builds
  debugImplementation(libs.ui.test.manifest)
  debugImplementation(libs.fragment.testing)

  // Annotation processors for test
  kaptTest(libs.hilt.android.compiler)
  kaptAndroidTest(libs.hilt.android.compiler)

  // Android test dependencies
  androidTestImplementation(libs.hilt.android.testing)
  androidTestImplementation(libs.junit.ktx)
  androidTestImplementation(libs.espresso.core)
  androidTestImplementation(libs.ui.test.junit4)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.core.ktx.test)
}
