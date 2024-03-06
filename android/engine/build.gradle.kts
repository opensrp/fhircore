// import DependenciesKt

plugins {
  `fhir-jacoco-report`
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("de.mannodermaus.android-junit5") version "1.9.3.0"
  id("dagger.hilt.android.plugin")
  id("org.jetbrains.kotlin.android")
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
  composeOptions { kotlinCompilerExtensionVersion = "1.4.8" }

  // CQL
  configurations {
    all {
      exclude(group = "org.eclipse.persistence")
      exclude(group = "javax.activation", module = "activation")
      exclude(group = "javax", module = "javaee-api")
      exclude(group = "xml-apis")
    }
  }
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

  configurations.configureEach {
    resolutionStrategy { force("ca.uhn.hapi.fhir:org.hl7.fhir.utilities:5.5.7") }
  }

  testCoverage { jacocoVersion = Deps.versions.jacoco_tool }
}

dependencies {
  implementation("androidx.preference:preference-ktx:1.2.0")
  coreLibraryDesugaring(Deps.desugar)

  api("androidx.core:core-ktx:1.9.0")
  api("androidx.appcompat:appcompat:1.6.1")
  api("com.google.android.material:material:1.8.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
  implementation("androidx.fragment:fragment-ktx:1.5.5")
  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("androidx.security:security-crypto:1.1.0-alpha03")
  api("org.smartregister:fhir-common-utils:0.0.8-SNAPSHOT")
  implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
  implementation("com.squareup.okhttp3:okhttp:4.9.1")
  implementation("androidx.cardview:cardview:1.0.0")
  implementation("joda-time:joda-time:2.10.14")
  implementation("androidx.paging:paging-runtime-ktx:3.1.1")
  implementation("com.github.bumptech.glide:glide:4.13.0")
  implementation("id.zelory:compressor:3.0.1")

  // CQL
  implementation(group = "xerces", name = "xercesImpl", version = "2.11.0")
  implementation(group = "com.github.java-json-tools", name = "msg-simple", version = "1.2")
  implementation("org.codehaus.woodstox:woodstox-core-asl:4.4.1")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-android:5.4.0")
  implementation("org.opencds.cqf.cql:engine:1.5.4")
  implementation("org.opencds.cqf.cql:engine.fhir:1.5.4")
  implementation("org.opencds.cqf.cql:evaluator:1.4.2")
  implementation("org.opencds.cqf.cql:evaluator.builder:1.4.2")
  implementation("org.opencds.cqf.cql:evaluator.activitydefinition:1.4.2")
  implementation("org.opencds.cqf.cql:evaluator.plandefinition:1.4.2")
  implementation("org.opencds.cqf.cql:evaluator.dagger:1.4.2")
  api("org.smartregister:workflow:0.1.0-alpha01-preview6-SNAPSHOT") {
    isTransitive = true
    exclude(group = "xerces")
    exclude(group = "com.github.java-json-tools")
    exclude(group = "org.codehaus.woodstox")
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "org.opencds.cqf.cql")
    exclude(group = "com.google.android.fhir", module = "common")
    //        exclude group: "com.google.android.fhir", module: "engine"
    exclude(group = "org.smartregister", module = "engine")
  }

  // Hilt - Dependency Injection
  implementation("com.google.dagger:hilt-android:${Deps.versions.hiltVersion}")
  api("androidx.hilt:hilt-work:${Deps.versions.hiltWorkerVersion}")
  kapt("androidx.hilt:hilt-compiler:${Deps.versions.hiltWorkerVersion}")
  kapt("com.google.dagger:hilt-compiler:${Deps.versions.hiltVersion}")

  api("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
  api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")

  // Jetpack - DataStore
  implementation("androidx.datastore:datastore-preferences:1.0.0")

  // P2P dependency
  api("org.smartregister:p2p-lib:0.3.0-SNAPSHOT")

  // Configure Jetpack Compose
  val composeVersion = Deps.versions.compose
  api("androidx.compose.ui:ui:$composeVersion")
  api("androidx.compose.ui:ui-tooling:$composeVersion")
  api("androidx.compose.foundation:foundation:1.3.1")
  api("androidx.compose.material:material:1.3.1")
  api("androidx.compose.material:material-icons-core:1.3.1")
  api("androidx.compose.material:material-icons-extended:1.3.1")
  api("androidx.compose.runtime:runtime-livedata:$composeVersion")
  api("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
  api("androidx.navigation:navigation-compose:2.5.3")
  api("androidx.hilt:hilt-navigation-compose:1.0.0")
  api("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
  api("androidx.paging:paging-compose:1.0.0-alpha18")
  api("androidx.activity:activity-compose:1.6.1")
  api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
  api("androidx.work:work-runtime-ktx:2.8.0")
  testApi("androidx.work:work-testing:2.8.0")

  val coroutineVersion = "1.7.3"
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion")
  api("org.smartregister:contrib-barcode:0.1.0-beta3-preview5-SNAPSHOT") {
    isTransitive = true
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
  }
  api("org.smartregister:data-capture:1.0.0-preview15-SNAPSHOT") {
    isTransitive = true
    exclude(group = "org.hamcrest", module = "hamcrest-core")
    exclude(group = "javax.xml.bind", module = "jaxb-api")
    exclude(group = "com.sun.xml.bind", module = "jaxb-core")
    exclude(group = "com.sun.activation", module = "javax.activation")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
  }

  api("org.smartregister:common:0.1.0-alpha03-preview4-SNAPSHOT") { isTransitive = true }

  api("org.smartregister:engine:0.1.0-beta04-preview4.2-SNAPSHOT") {
    isTransitive = true
    exclude(group = "org.hamcrest", module = "hamcrest-core")
    exclude(group = "javax.xml.bind", module = "jaxb-api")
    exclude(group = "com.sun.xml.bind", module = "jaxb-core")
    exclude(group = "com.sun.activation", module = "javax.activation")
    exclude(group = "org.opencds.cqf.cql", module = "engine")
    exclude(group = "org.opencds.cqf.cql", module = "evaluator")
    exclude(group = "org.opencds.cqf.cql", module = "evaluator.builder")
    exclude(group = "org.opencds.cqf.cql", module = "evaluator.dagger")
    exclude(group = "com.google.android.fhir", module = "common")
  }

  api("com.google.code.gson:gson:2.9.1")
  api("com.jakewharton.timber:timber:5.0.1")

  val retrofitVersion = "2.9.0"

  api("com.squareup.retrofit2:retrofit:$retrofitVersion")
  api("com.squareup.retrofit2:converter-gson:$retrofitVersion")
  api("com.squareup.retrofit2:retrofit-mock:$retrofitVersion")
  api("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

  val okhttpVersion = "4.9.1"
  api("com.squareup.okhttp3:okhttp:$okhttpVersion")
  api("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

  implementation(platform("com.google.firebase:firebase-bom:31.2.0"))
  implementation("com.google.firebase:firebase-perf-ktx")

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

  debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion)")
  testImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")

  debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
  testImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}

kapt { correctErrorTypes = true }

hilt { enableAggregatingTask = true }
