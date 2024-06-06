import Dependencies.removeIncompatibleDependencies

plugins {
  `fhir-jacoco-report`
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("de.mannodermaus.android-junit5")
  id("com.google.dagger.hilt.android")
  id("org.jetbrains.kotlin.android")
  id("com.google.firebase.crashlytics")
}

kotlin { jvmToolchain(17) }

android {
  namespace = "org.smartregister.fhircore.engine"
  compileSdk = Deps.sdk_versions.compile_sdk

  defaultConfig {
    minSdk = Deps.sdk_versions.min_sdk
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    debug { enableUnitTestCoverage = true }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions { isCoreLibraryDesugaringEnabled = true }
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
  }
  buildFeatures {
    compose = true
    viewBinding = true
    dataBinding = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

  packaging {
    resources {
      excludes +=
        listOf(
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
        )
    }
  }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true

    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  testCoverage { jacocoVersion = Deps.versions.jacoco_tool }
}

configurations {
  all {
    exclude(group = "org.eclipse.persistence")
    exclude(group = "javax.activation", module = "activation")
    exclude(group = "javax", module = "javaee-api")
    exclude(group = "xml-apis")
    exclude(group = "xpp3")
    removeIncompatibleDependencies()
  }
}

dependencies {
  implementation("androidx.preference:preference-ktx:1.2.1")
  coreLibraryDesugaring(Deps.desugar)

  api("androidx.core:core-ktx:1.13.1")
  api("androidx.appcompat:appcompat:1.7.0")
  api("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
  implementation("androidx.fragment:fragment-ktx:1.7.1")
  api("io.jsonwebtoken:jjwt:0.9.1")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  api("org.smartregister:fhir-common-utils:0.0.8-SNAPSHOT") {
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("androidx.cardview:cardview:1.0.0")
  implementation("joda-time:joda-time:2.10.14")
  implementation("androidx.paging:paging-runtime-ktx:3.3.0")
  implementation("com.github.bumptech.glide:glide:4.16.0")
  implementation("id.zelory:compressor:3.0.1")

  implementation(group = "javax.xml.stream", name = "stax-api", version = "1.0-2")
  implementation(group = "xerces", name = "xercesImpl", version = "2.11.0")
  implementation(group = "com.github.java-json-tools", name = "msg-simple", version = "1.2")

  implementation("org.opencds.cqf.fhir:cqf-fhir-cr:3.0.0-PRE9") {
    isTransitive = true
    exclude(group = "org.codelibs", module = "xpp3")
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }

  api("org.smartregister:knowledge:0.1.0-alpha03-preview4-SNAPSHOT") {
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }

  api("org.smartregister:workflow:0.1.0-alpha04-preview8-SNAPSHOT") {
    isTransitive = true
    exclude(group = "xerces")
    exclude(group = "com.github.java-json-tools")
    exclude(group = "org.codehaus.woodstox")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "org.smartregister", module = "knowledge")
  }

  // Hilt - Dependency Injection
  implementation("com.google.dagger:hilt-android:${Deps.versions.hiltVersion}")
  kapt("com.google.dagger:hilt-compiler:${Deps.versions.hiltVersion}")

  api("androidx.hilt:hilt-work:${Deps.versions.hiltWorkerVersion}")
  kapt("androidx.hilt:hilt-compiler:${Deps.versions.hiltWorkerVersion}")

  api("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
  api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

  // Jetpack - DataStore
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // P2P dependency
  api("org.smartregister:p2p-lib:0.3.0-SNAPSHOT")

  // Configure Jetpack Compose
  api(platform("androidx.compose:compose-bom:2024.05.00"))
  api("androidx.compose.ui:ui")
  api("androidx.compose.ui:ui-tooling")
  api("androidx.compose.foundation:foundation")
  api("androidx.compose.material:material")
  api("androidx.compose.material:material-icons-core")
  api("androidx.compose.material:material-icons-extended")
  api("androidx.compose.runtime:runtime-livedata")
  api("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
  api("androidx.navigation:navigation-compose:2.7.7")
  api("androidx.hilt:hilt-navigation-compose:1.2.0")
  api("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  api("androidx.paging:paging-compose:3.3.0")
  api("androidx.activity:activity-compose:1.9.0")
  api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
  api("androidx.work:work-runtime-ktx:2.9.0")
  testApi("androidx.work:work-testing:2.9.0")

  val coroutineVersion = "1.8.1"
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion")
  api("org.smartregister:contrib-barcode:0.1.0-beta3-preview7-SNAPSHOT") {
    isTransitive = true
    exclude(group = "org.smartregister", module = "data-capture")
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
  }
  api("org.smartregister:data-capture:1.1.0-preview8.1-SNAPSHOT") {
    isTransitive = true
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "engine")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }

  api("org.smartregister:common:0.1.0-alpha05-preview3-SNAPSHOT") { isTransitive = true }

  //  api("org.smartregister:engine:1.0.0-preview7.1-SNAPSHOT") {
  api("org.smartregister:engine:1.0.0-preview8-PERF-TEST-SNAPSHOT") {
    //    api("org.smartregister:engine:1.0.0-preview7.1-PERF-TEST5-SNAPSHOT") {
    isTransitive = true
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "org.smartregister", module = "knowledge")
  }

  api("com.google.code.gson:gson:2.10.1")
  api("com.jakewharton.timber:timber:5.0.1")

  val retrofitVersion = "2.9.0"

  api("com.squareup.retrofit2:retrofit:$retrofitVersion")
  api("com.squareup.retrofit2:converter-gson:$retrofitVersion")
  api("com.squareup.retrofit2:retrofit-mock:$retrofitVersion")
  api("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

  val okhttpVersion = "4.12.0"
  api("com.squareup.okhttp3:okhttp:$okhttpVersion")
  api("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

  implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
  implementation("com.google.firebase:firebase-perf-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-analytics")

  implementation("androidx.core:core-splashscreen:1.0.1")

  // Hilt test dependencies
  testImplementation("com.google.dagger:hilt-android-testing:${Deps.versions.hiltVersion}")
  kaptTest("com.google.dagger:hilt-android-compiler:${Deps.versions.hiltVersion}")

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
  testImplementation(Deps.kotlin.test)
  testImplementation(Deps.mockk)
  androidTestImplementation(Deps.atsl.ext_junit)
  androidTestImplementation(Deps.atsl.espresso)
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
  androidTestImplementation(Deps.atsl.ext_junit)
  androidTestImplementation("androidx.test.espresso:espresso-core:${Deps.versions.atsl_expresso}")
  implementation(Deps.work.runtime)
  testImplementation(group = "org.json", name = "json", version = "20210307")

  debugImplementation("androidx.compose.ui:ui-test-manifest")
  testImplementation("androidx.compose.ui:ui-test-junit4:")

  debugImplementation("androidx.compose.ui:ui-test-manifest:")
  testImplementation("androidx.compose.ui:ui-test-junit4:")

  /**
   * This is an SDK Dependency graph bug workaround file HAPI FHIR Dependencies missing at runtime
   * after building FHIRCore application module
   *
   * To be included in the engine/build.gradle.kts file via apply {}
   */
  implementation(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }
  implementation(Dependencies.HapiFhir.guavaCaching)
  implementation(Dependencies.HapiFhir.validationR4)
  implementation(Dependencies.HapiFhir.validation) {
    exclude(module = "commons-logging")
    exclude(module = "httpclient")
  }

  constraints {
    Dependencies.hapiFhirConstraints().forEach { (libName, constraints) ->
      api(libName, constraints)
    }
  }

  /** End SDK Dependency Graph workaround * */
}

kapt { correctErrorTypes = true }

hilt { enableAggregatingTask = true }
