plugins {
  `fhir-properties`
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
  namespace = "org.dtree.fhircore.dataclerk"
  compileSdk = Deps.sdk_versions.compile_sdk

  defaultConfig {
    applicationId = "org.dtree.fhircore.dataclerk"
    minSdk = Deps.sdk_versions.min_sdk
    targetSdk = Deps.sdk_versions.target_sdk
    versionCode = 4
    versionName = "1.0.1"
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
    vectorDrawables { useSupportLibrary = true }
  }

  //    signingConfigs {
  //        release {
  //
  //            v1SigningEnabled false
  //            v2SigningEnabled true
  //
  //            keyAlias System.getenv("KEYSTORE_ALIAS")?: project.KEYSTORE_ALIAS
  //            keyPassword System.getenv("KEY_PASSWORD") ?: project.KEY_PASSWORD
  //            storePassword System.getenv("KEYSTORE_PASSWORD") ?: project.KEYSTORE_PASSWORD
  //            storeFile file(System.getProperty("user.home") + "/fhircore.keystore.jks")
  //        }
  //    }

  buildTypes {
    debug {
      //            enableUnitTestCoverage true
      resValue("string", "authenticator_account_type", "\"${android.defaultConfig.applicationId}\"")
    }
    release {
      resValue("string", "authenticator_account_type", "\"${android.defaultConfig.applicationId}\"")
      //            minifyEnabled false
      //            proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"),
      // "proguard-rules.pro"
      //            signingConfig signingConfigs.release
      //            firebaseCrashlytics {
      //                nativeSymbolUploadEnabled false
      //            }
    }
  }

  compileOptions { isCoreLibraryDesugaringEnabled = true }

  kotlinOptions {
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = "1.4.8" }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true

    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
      all {
        //                beforeTest {
        //                    println("${it.className} > ${it.name} STARTED")
        //                }
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

  configurations.configureEach {
    resolutionStrategy { force("ca.uhn.hapi.fhir:org.hl7.fhir.utilities:5.5.7") }
  }

  lint { abortOnError = false }

  testCoverage { jacocoVersion = Deps.versions.jacoco_tool }
}

dependencies {
  coreLibraryDesugaring(Deps.desugar)
  implementation(project(":engine"))

  implementation("androidx.core:core-ktx:1.8.0")
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
  implementation("androidx.activity:activity-compose:1.5.1")

  implementation(Deps.accompanist.swiperefresh)

  implementation(platform("androidx.compose:compose-bom:2022.10.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.paging:paging-compose:3.2.0")

  // Hilt - Dependency Injection
  implementation("com.google.dagger:hilt-android:${Deps.versions.hiltVersion}")
  kapt("com.google.dagger:hilt-compiler:${Deps.versions.hiltVersion}")

  // analytics
  implementation(platform("com.google.firebase:firebase-bom:31.2.0"))

  implementation("com.google.firebase:firebase-perf-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-analytics-ktx")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2022.10.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt { correctErrorTypes = true }

hilt { enableAggregatingTask = true }
