import Dependencies.removeIncompatibleDependencies
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  `jacoco-report`
  ktlint
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
  compileSdk = BuildConfigs.compileSdk

  namespace = "org.smartregister.fhircore.engine"

  defaultConfig {
    minSdk = BuildConfigs.minSdk
    testInstrumentationRunner = "org.smartregister.fhircore.engine.EngineTestRunner"
    consumerProguardFiles("consumer-rules.pro")
    buildConfigField(
      "boolean",
      "IS_NON_PROXY_APK",
      "${project.hasProperty("isNonProxy") && property("isNonProxy").toString().toBoolean()}",
    )
  }

  buildTypes {
    getByName("debug") {
      enableUnitTestCoverage = BuildConfigs.enableUnitTestCoverage
      enableAndroidTestCoverage = BuildConfigs.enableAndroidTestCoverage
    }

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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
  buildFeatures {
    compose = true
    viewBinding = true
    dataBinding = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = BuildConfigs.kotlinCompilerExtensionVersion }

  packaging {
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

  lint { baseline = file("lint-baseline.xml") }

  testCoverage { jacocoVersion = BuildConfigs.jacocoVersion }
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
    removeIncompatibleDependencies()
  }
}

dependencies {
  implementation(libs.play.services.tasks)
  implementation(libs.gms.play.services.location)
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
  implementation(libs.dagger.hilt.android)
  implementation(libs.hilt.work)
  implementation(libs.slf4j.nop)
  implementation(libs.fhir.sdk.common)

  // Shared dependencies
  api(libs.bundles.datastore.kt)
  api(libs.bundles.navigation)
  api(libs.bundles.materialicons)
  api(libs.bundles.compose)
  api(libs.bundles.lifecycle)
  api(libs.bundles.accompanist)
  api(libs.bundles.coroutines)
  api(libs.bundles.retrofit2)
  api(libs.bundles.okhttp3)
  api(libs.bundles.paging)
  api(libs.ui)
  api(libs.glide)
  api(libs.knowledge) { exclude(group = "org.slf4j", module = "jcl-over-slf4j") }
  api(libs.p2p.lib)
  api(libs.java.jwt)
  api(libs.fhir.common.utils) { exclude(group = "org.slf4j", module = "jcl-over-slf4j") }
  api(libs.runtime.livedata)
  api(libs.foundation)
  api(libs.kotlinx.serialization.json)
  api(libs.work.runtime.ktx)
  api(libs.prettytime)
  api(libs.kotlin.reflect)
  api(libs.stax.api)
  api(libs.gson)
  api(libs.timber)
  api(libs.converter.gson)
  api(libs.json.path)
  api(libs.easy.rules.jexl) { exclude(group = "commons-logging", module = "commons-logging") }
  api(libs.data.capture) {
    isTransitive = true
    exclude(group = "ca.uhn.hapi.fhir")
    exclude(group = "com.google.android.fhir", module = "common")
    exclude(group = "org.smartregister", module = "common")
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }
  api(libs.cqf.fhir.cr) {
    isTransitive = true
    exclude(group = "org.codelibs", module = "xpp3")
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }
  api(libs.workflow) {
    isTransitive = true
    exclude(group = "xerces")
    exclude(group = "com.github.java-json-tools")
    exclude(group = "org.codehaus.woodstox")
    exclude(group = "com.google.android.fhir", module = "engine")
    exclude(group = "org.smartregister", module = "engine")
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "com.google.android.fhir", module = "knowledge")
  }
  api(libs.contrib.barcode) {
    isTransitive = true
    exclude(group = "org.smartregister", module = "data-capture")
    exclude(group = "ca.uhn.hapi.fhir")
  }
  api(libs.contrib.locationwidget) {
    isTransitive = true
    exclude(group = "org.smartregister", module = "data-capture")
    exclude(group = "ca.uhn.hapi.fhir")
  }
  api(libs.fhir.engine) {
    isTransitive = true
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "com.google.android.fhir", module = "common")
  }

  // Annotation processors
  kapt(libs.hilt.compiler)
  kapt(libs.dagger.hilt.compiler)

  // Annotation processors for test
  kaptTest(libs.dagger.hilt.android.compiler)
  kaptAndroidTest(libs.dagger.hilt.android.compiler)

  testRuntimeOnly(libs.bundles.junit.jupiter.runtime)

  // Test dependencies
  testImplementation(libs.work.runtime.ktx)
  testImplementation(libs.dagger.hilt.android.testing)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.robolectric)
  testImplementation(libs.bundles.junit.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.core.testing)
  testImplementation(libs.mockk)
  testImplementation(libs.json)
  testImplementation(libs.navigation.testing)
  testImplementation(libs.work.testing)

  // To run only on debug builds
  debugImplementation(libs.ui.test.manifest)
  debugImplementation(libs.fragment.testing)
  debugImplementation(libs.ui.tooling)

  // Android test dependencies
  androidTestImplementation(libs.bundles.junit.test)
  androidTestImplementation(libs.runner)
  androidTestImplementation(libs.ui.test.junit4)
  androidTestImplementation(libs.dagger.hilt.android.testing)
  androidTestImplementation(libs.benchmark.junit)

  /**
   * This is an SDK Dependency graph bug workaround file HAPI FHIR Dependencies missing at runtime
   * after building FHIRCore application module
   *
   * To be included in the engine/build.gradle.kts file via apply {}
   */
  implementation(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }
  implementation(Dependencies.HapiFhir.guavaCaching)
  implementation(Dependencies.HapiFhir.validationR4)
  implementation(Dependencies.HapiFhir.validationR5)
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
  ktlint(libs.ktlint.main) {
    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
  }
  ktlint(project(":linting"))
}
