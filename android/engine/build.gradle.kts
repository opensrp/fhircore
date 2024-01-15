import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
  apply(from = "../jacoco.gradle.kts")
  apply(from = "../ktlint.gradle.kts")
}

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
  id("kotlin-parcelize")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("de.mannodermaus.android-junit5")
  id("dagger.hilt.android.plugin")
  id("androidx.navigation.safeargs")
}

android {
  compileSdk = 34

  defaultConfig {
    minSdk = 26
    targetSdk = 34
    testInstrumentationRunner = "org.smartregister.fhircore.engine.EngineTestRunner"
    consumerProguardFiles("consumer-rules.pro")
    buildConfigField(
      "boolean",
      "IS_NON_PROXY_APK",
      "${project.hasProperty("isNonProxy") && property("isNonProxy").toString().toBoolean()}",
    )
  }

  buildTypes {
    getByName("debug") { isTestCoverageEnabled = true }

    create("debugNonProxy") {
      initWith(getByName("debug"))
      buildConfigField(
        "boolean",
        "IS_NON_PROXY_APK",
        "true",
      )
    }

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
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
  buildFeatures {
    compose = true
    viewBinding = true
    dataBinding = true
  }
  composeOptions { kotlinCompilerExtensionVersion = "1.4.3" }

  packagingOptions {
    resources.excludes.addAll(
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
        "META-INF/INDEX.LIST",
      ),
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
  testLogging { events = setOf(TestLogEvent.FAILED) }
  minHeapSize = "4608m"
  maxHeapSize = "4608m"
}

configurations {
  all {
    exclude(group = "org.eclipse.persistence")
    exclude(group = "javax.activation", module = "activation")
    exclude(group = "javax", module = "javaee-api")
    exclude(group = "xml-apis")
    exclude(group = "xpp3")
  }
}

dependencies {
  coreLibraryDesugaring(libs.core.desugar)

  // Library dependencies
  implementation(libs.preference.ktx)
  implementation(libs.core.ktx)
  implementation(libs.appcompat)
  implementation(libs.material)
  implementation(libs.constraintlayout)
  implementation(libs.fragment.ktx)
  implementation(libs.security.crypto)
  implementation(libs.cardview)
  implementation(libs.joda.time)
  implementation(libs.paging.runtime.ktx)
  implementation(libs.compressor)
  implementation(libs.xercesImpl)
  implementation(libs.msg.simple)
  implementation(libs.cqf.cql.engine)
  implementation(libs.cql.engine.jackson)
  implementation(libs.dagger.hilt.android)
  implementation(libs.hilt.work)
  implementation(libs.slf4j.nop)
  implementation(libs.cqf.cql.evaluator) {
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "ca.uhn.hapi.fhir")
  }
  implementation(libs.cql.evaluator.builder) {
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "ca.uhn.hapi.fhir")
  }
  implementation(libs.cql.evaluator.plandefinition) {
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "ca.uhn.hapi.fhir")
  }
  implementation(libs.cql.evaluator.dagger) {
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "ca.uhn.hapi.fhir")
  }

  // Shared dependencies
  api(libs.bundles.datastore.kt)
  api(libs.glide)
  api(libs.knowledger)
  api(libs.p2p.lib)
  api(libs.jjwt)
  api(libs.fhir.common.utils)
  api(libs.bundles.navigation)
  api(libs.bundles.materialicons)
  api(libs.bundles.compose)
  api(libs.hilt.navigation.compose)
  api(libs.bundles.lifecycle)
  api(libs.paging.compose)
  api(libs.kotlinx.serialization.json)
  api(libs.bundles.accompanist)
  api(libs.work.runtime.ktx)
  api(libs.prettytime)
  api(libs.bundles.coroutines)
  api(libs.kotlin.reflect)
  api(libs.stax.api)
  api(libs.caffeine)
  api(libs.gson)
  api(libs.timber)
  api(libs.bundles.retrofit2)
  api(libs.converter.gson)
  api(libs.bundles.okhttp3)
  api(libs.json.path)
  api(libs.commons.jexl3) { exclude(group = "commons-logging", module = "commons-logging") }
  api(libs.easy.rules.jexl) {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "org.apache.commons", module = "commons-jexl3")
  }
  api(libs.data.capture) {
    isTransitive = true
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "engine")
  }
  api(libs.workflow) {
    isTransitive = true
    exclude(group = "xerces")
    exclude(group = "com.github.java-json-tools")
    exclude(group = "org.codehaus.woodstox")
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
    exclude(group = "com.github.ben-manes.caffeine")
  }
  api(libs.contrib.barcode) {
    isTransitive = true
    exclude(group = "org.smartregister", module = "data-capture")
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.google.android.fhir", module = "engine")
  }
  api(libs.fhir.engine) {
    isTransitive = true
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "com.github.ben-manes.caffeine")
  }

  // Annotation processors
  kapt(libs.hilt.compiler)
  kapt(libs.dagger.hilt.compiler)

  // Annotation processors for test
  kaptTest(libs.hilt.android.compiler)
  kaptAndroidTest(libs.hilt.android.compiler)

  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.vintage.engine)

  // Test dependencies
  testImplementation(libs.work.runtime.ktx)
  testImplementation(libs.hilt.android.testing)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.robolectric)
  testImplementation(libs.bundles.junit.test)
  testImplementation(libs.bundles.coroutine.test)
  testImplementation(libs.core.testing)
  testImplementation(libs.mockk)
  testImplementation(libs.json)
  testImplementation(libs.navigation.testing)
  testImplementation(libs.work.testing)

  // To run only on debug builds
  debugImplementation(libs.ui.test.manifest)
  debugImplementation(libs.fragment.testing)

  // Android test dependencies
  androidTestImplementation(libs.bundles.junit.test)
  androidTestImplementation(libs.runner)
  androidTestImplementation(libs.ui.test.junit4)
  androidTestImplementation(libs.hilt.android.testing)
  androidTestImplementation(libs.benchmark.junit)

  ktlint(libs.ktlint.main) {
    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
  }
  ktlint(project(":linting"))
}
