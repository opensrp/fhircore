import android.databinding.tool.ext.capitalizeUS
import com.android.build.api.variant.FilterConfiguration.FilterType
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.json.JSONArray
import org.json.JSONObject

plugins {
  `jacoco-report`
  `project-properties`
  ktlint
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("kotlin-parcelize")
  id("de.mannodermaus.android-junit5")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("dagger.hilt.android.plugin")
  id("androidx.navigation.safeargs")
  id("org.sonarqube") version "3.5.0.2730"
  id("io.sentry.android.gradle") version "3.5.0"
}

sonar {
  properties {
    property("sonar.projectKey", "fhircore")
    property("sonar.kotlin.source.version", libs.versions.kotlin)
    property(
      "sonar.androidLint.reportPaths",
      "${project.layout.buildDirectory.get()}/reports/lint-results-opensrpDebug.xml",
    )
    property("sonar.host.url", System.getenv("SONAR_HOST_URL"))
    property("sonar.login", System.getenv("SONAR_TOKEN"))
    property("sonar.sourceEncoding", "UTF-8")
    property(
      "sonar.kotlin.threads",
      (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1,
    )
    property(
      "sonar.exclusions",
      """**/*Test*/**,
          **/*test*/**,
          **/.gradle/**,
          **/R.class,
          *.json,
          *.yaml""",
    )
  }
}

android {
  compileSdk = BuildConfigs.compileSdk

  val buildDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

  namespace = "org.smartregister.fhircore.quest"

  defaultConfig {
    applicationId = BuildConfigs.applicationId
    minSdk = BuildConfigs.minSdk
    targetSdk = BuildConfigs.targetSdk
    versionCode = BuildConfigs.versionCode
    versionName = BuildConfigs.versionName
    multiDexEnabled = true

    buildConfigField("boolean", "SKIP_AUTH_CHECK", "false")
    buildConfigField("String", "FHIR_BASE_URL", """"${project.extra["FHIR_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_BASE_URL", """"${project.extra["OAUTH_BASE_URL"]}"""")
    buildConfigField("String", "OAUTH_CLIENT_ID", """"${project.extra["OAUTH_CLIENT_ID"]}"""")
    buildConfigField("String", "OAUTH_SCOPE", """"${project.extra["OAUTH_SCOPE"]}"""")
    buildConfigField("String", "OPENSRP_APP_ID", """${project.extra["OPENSRP_APP_ID"]}""")
    buildConfigField("String", "CONFIGURATION_SYNC_PAGE_SIZE", """"100"""")
    buildConfigField("String", "SENTRY_DSN", """"${project.extra["SENTRY_DSN"]}"""")
    buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")

    testInstrumentationRunner = "org.smartregister.fhircore.quest.QuestTestRunner"
    testInstrumentationRunnerArguments["additionalTestOutputDir"] = "/sdcard/Download"
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
      "ACTIVITY-MISSING,CODE-COVERAGE,DEBUGGABLE,UNLOCKED,EMULATOR"

    // The following argument makes the Android Test Orchestrator run its
    // "pm clear" command after each test invocation. This command ensures
    // that the app's state is completely cleared between tests.
    testInstrumentationRunnerArguments["clearPackageData"] = "true"
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
    getByName("debug") {
      enableUnitTestCoverage = BuildConfigs.enableUnitTestCoverage
      enableAndroidTestCoverage = BuildConfigs.enableAndroidTestCoverage
    }

    create("debugNonProxy") { initWith(getByName("debug")) }

    create("benchmark") {
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("debug")
      isDebuggable = true
    }

    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      signingConfig = signingConfigs.getByName("release")
    }
  }

  packaging {
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
        "META-INF/INDEX.LIST",
      ),
    )
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
  }

  buildFeatures {
    compose = true
    viewBinding = true
    dataBinding = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = BuildConfigs.kotlinCompilerExtensionVersion }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true

    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  testCoverage { jacocoVersion = BuildConfigs.jacocoVersion }

  lint { abortOnError = false }

  flavorDimensions += "apps"

  productFlavors {
    create("opensrp") {
      dimension = "apps"
      manifestPlaceholders["appLabel"] = "OpenSRP"
      isDefault = true
    }

    create("ecbis") {
      dimension = "apps"
      applicationIdSuffix = ".ecbis"
      versionNameSuffix = "-ecbis"
      manifestPlaceholders["appLabel"] = "MOH eCBIS"
    }

    create("ecbis_preview") {
      dimension = "apps"
      applicationIdSuffix = ".ecbis_preview"
      versionNameSuffix = "-ecbis_preview"
      manifestPlaceholders["appLabel"] = "MOH eCBIS Preview"
    }

    create("g6pd") {
      dimension = "apps"
      applicationIdSuffix = ".g6pd"
      versionNameSuffix = "-g6pd"
      manifestPlaceholders["appLabel"] = "G6PD"
    }

    create("mwcore") {
      dimension = "apps"
      applicationIdSuffix = ".mwcore"
      versionNameSuffix = "-mwcore"
      manifestPlaceholders["appLabel"] = "Malawi Core"
    }

    create("afyayangu") {
      dimension = "apps"
      applicationIdSuffix = ".afyayangu"
      versionNameSuffix = "-afyayangu"
      manifestPlaceholders["appLabel"] = "Afya Yangu"
    }

    create("map") {
      dimension = "apps"
      applicationIdSuffix = ".map"
      versionNameSuffix = "-map"
      manifestPlaceholders["appLabel"] = "Geo Widget"
    }

    create("echis") {
      dimension = "apps"
      applicationIdSuffix = ".echis"
      versionNameSuffix = "-echis"
      manifestPlaceholders["appLabel"] = "MOH eCHIS"
    }

    create("sidBunda") {
      dimension = "apps"
      applicationIdSuffix = ".sidBunda"
      versionNameSuffix = "-sidBunda"
      manifestPlaceholders["appLabel"] = "BidanKu"
    }

    create("sidCadre") {
      dimension = "apps"
      applicationIdSuffix = ".sidCadre"
      versionNameSuffix = "-sidCadre"
      manifestPlaceholders["appLabel"] = "KaderKu"
    }

    create("sidEir") {
      dimension = "apps"
      applicationIdSuffix = ".sidEir"
      versionNameSuffix = "-sidEir"
      manifestPlaceholders["appLabel"] = "VaksinatorKu"
    }

    create("sidEcd") {
      dimension = "apps"
      applicationIdSuffix = ".sidEcd"
      versionNameSuffix = "-sidEcd"
      manifestPlaceholders["appLabel"] = "PaudKu"
    }

    create("diabetesCompass") {
      dimension = "apps"
      applicationIdSuffix = ".diabetesCompass"
      versionNameSuffix = "-diabetesCompass"
      manifestPlaceholders["appLabel"] = "Diabetes Compass"
    }

    create("diabetesCompassClinic") {
      dimension = "apps"
      applicationIdSuffix = ".diabetesCompassClinic"
      versionNameSuffix = "-diabetesCompassClinic"
      manifestPlaceholders["appLabel"] = "Diabetes Compass Clinic"
    }

    create("zeir") {
      dimension = "apps"
      applicationIdSuffix = ".zeir"
      versionNameSuffix = "-zeir"
      manifestPlaceholders["appLabel"] = "ZEIR"
    }

    create("gizEir") {
      dimension = "apps"
      applicationIdSuffix = ".gizeir"
      versionNameSuffix = "-gizeir"
      manifestPlaceholders["appLabel"] = "EIR"
    }

    create("engage") {
      dimension = "apps"
      applicationIdSuffix = ".engage"
      versionNameSuffix = "-engage"
      manifestPlaceholders["appLabel"] = "Engage"
    }

    create("eir") {
      dimension = "apps"
      applicationIdSuffix = ".who_eir"
      versionNameSuffix = "-who_eir"
      manifestPlaceholders["appLabel"] = "WHO EIR"
    }

    create("psi-eswatini") {
      dimension = "apps"
      applicationIdSuffix = ".psi_eswatini"
      versionNameSuffix = "-psi_eswatini"
      manifestPlaceholders["appLabel"] = "PSI WFA"
    }

    create("eusmMg") {
      dimension = "apps"
      applicationIdSuffix = ".eusmMg"
      versionNameSuffix = "-eusmMg"
      manifestPlaceholders["appLabel"] = "EUSM Madagascar"
    }

    create("eusmBi") {
      dimension = "apps"
      applicationIdSuffix = ".eusmBi"
      versionNameSuffix = "-eusmBi"
      manifestPlaceholders["appLabel"] = "EUSM Burundi"
    }

    create("demoEir") {
      dimension = "apps"
      applicationIdSuffix = ".demoEir"
      versionNameSuffix = "-demoEir"
      manifestPlaceholders["appLabel"] = "OpenSRP EIR"
    }

    create("contigo") {
      dimension = "apps"
      applicationIdSuffix = ".contigo"
      versionNameSuffix = "-contigo"
      manifestPlaceholders["appLabel"] = "Contigo"
    }

    create("minsaEir") {
      dimension = "apps"
      applicationIdSuffix = ".minsaEir"
      versionNameSuffix = "-minsaEir"
      manifestPlaceholders["appLabel"] = "Minsa EIR"
    }
  }

  applicationVariants.all {
    val variant = this
    variant.resValue("string", "authenticator_account_type", "\"${applicationId}\"")
    variant.resValue(
      "string",
      "app_name",
      "\"${variant.mergedFlavor.manifestPlaceholders["appLabel"]}\"",
    )
  }

  applicationVariants.all {
    val variant = this
    tasks.register("jacocoTestReport${variant.name.capitalizeUS()}")
  }

  splits {
    abi {
      isEnable = false

      reset()

      // A list of ABIs for Gradle to create APKs for.
      include("armeabi-v7a", "x86", "arm64-v8a", "x86_64")

      isUniversalApk = false
    }
  }
}

val abiCodes =
  mapOf(
    "armeabi" to 1,
    "armeabi-v7a" to 2,
    "arm64-v8a" to 3,
    "mips" to 4,
    "x86" to 5,
    "x86_64" to 6,
  )

// For each APK output variant, override versionCode with a combination of
// ext.abiCodes * 10000 + variant.versionCode. In this example, variant.versionCode
// is equal to defaultConfig.versionCode. If you configure product flavors that
// define their own versionCode, variant.versionCode uses that value instead.
androidComponents {
  onVariants { variant ->
    variant.outputs.forEach { output ->
      val name = output.filters.find { it.filterType == FilterType.ABI }?.identifier

      // Stores the value of ext.abiCodes that is associated with the ABI for this variant.
      val baseAbiCode = abiCodes[name]
      // Because abiCodes.get() returns null for ABIs that are not mapped by ext.abiCodes,
      // the following code doesn't override the version code for universal APKs.
      // However, because you want universal APKs to have the lowest version code,
      // this outcome is desirable.
      if (baseAbiCode != null) {
        // Assigns the new version code to versionCodeOverride, which changes the
        // version code for only the output APK, not for the variant itself. Skipping
        // this step causes Gradle to use the value of variant.versionCode for the APK.
        output.versionCode.set(baseAbiCode * 10000 + (output.versionCode.orNull ?: 0))
      }
    }
  }
}

tasks.withType<Test> {
  testLogging { events = setOf(TestLogEvent.FAILED) }
  minHeapSize = "4608m"
  maxHeapSize = "4608m"
  // maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
  configure<JacocoTaskExtension> { isIncludeNoLocationClasses = true }
}

configurations { all { exclude(group = "xpp3") } }

dependencies {
  implementation(libs.gms.play.services.location)
  coreLibraryDesugaring(libs.core.desugar)

  // Application dependencies
  implementation(project(":engine"))
  implementation(project(":geowidget")) { isTransitive = true }
  implementation(libs.core.ktx)
  implementation(libs.appcompat)
  implementation(libs.material)
  implementation(libs.dagger.hilt.android)
  implementation(libs.hilt.work)
  implementation(libs.mlkit.barcode.scanning)

  implementation(libs.bundles.cameraX)

  implementation("com.github.anrwatchdog:anrwatchdog:1.4.0")

  // Annotation processors
  kapt(libs.hilt.compiler)
  kapt(libs.dagger.hilt.compiler)

  testRuntimeOnly(libs.bundles.junit.jupiter.runtime)

  // Unit test dependencies
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.robolectric)
  testImplementation(libs.bundles.junit.test)
  testImplementation(libs.core.testing)
  testImplementation(libs.mockk)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.dagger.hilt.android.testing)
  testImplementation(libs.navigation.testing)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.work.testing)

  // To run only on debug builds
  debugImplementation(libs.ui.test.manifest)
  debugImplementation(libs.fragment.testing)
  //    debugImplementation(libs.leakcanary.android)

  // Annotation processors for test
  kaptTest(libs.dagger.hilt.android.compiler)
  kaptAndroidTest(libs.dagger.hilt.android.compiler)

  androidTestUtil(libs.orchestrator)

  // Android test dependencies
  androidTestImplementation(libs.bundles.junit.test)
  androidTestImplementation(libs.runner)
  androidTestImplementation(libs.ui.test.junit4)
  androidTestImplementation(libs.dagger.hilt.android.testing)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.benchmark.junit)
  androidTestImplementation(libs.work.testing)
  androidTestImplementation(libs.navigation.testing)
  // Android Test dependencies
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.espresso.core)
  androidTestImplementation(libs.rules)
  androidTestImplementation(libs.uiautomator)

  ktlint(libs.ktlint.main) {
    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
  }
  ktlint(project(":linting"))
}

/**
 * This task compares the performance benchmark results to the expected benchmark results and throws
 * an error if the result is past the expected result and margin. A message will also be printed if
 * the performance significantly improves.
 */
task("evaluatePerformanceBenchmarkResults") {
  val expectedPerformanceLimitsFile = project.file("expected-results.json")
  val resultsFile = project.file("org.smartregister.opensrp.ecbis-benchmarkData.json")

  doLast {
    if (resultsFile.exists()) {
      // Read the expectations file
      val expectedResultsMap: HashMap<String, HashMap<String, Double>> = hashMapOf()

      JSONObject(FileReader(expectedPerformanceLimitsFile).readText()).run {
        keys().forEach { key ->
          val resultMaxDeltaMap: HashMap<String, Double> = hashMapOf()
          val methodExpectedResults = this.getJSONObject(key.toString())

          methodExpectedResults.keys().forEach { expectedResultsKey ->
            resultMaxDeltaMap.put(
              expectedResultsKey.toString(),
              methodExpectedResults.getDouble(expectedResultsKey.toString()),
            )
          }

          expectedResultsMap[key.toString()] = resultMaxDeltaMap
        }
      }

      // Loop through the results file updating the results
      JSONObject(FileReader(resultsFile).readText()).run {
        getJSONArray("benchmarks").iterator().forEach { any ->
          val benchmarkResult = any as JSONObject
          val fullName = benchmarkResult.getTestName()
          val timings = benchmarkResult.getJSONObject("metrics").getJSONObject("timeNs")

          val median = timings.getDouble("median")
          val expectedTimings = expectedResultsMap[fullName]

          if (expectedTimings == null) {
            System.err.println(
              "Metrics for $fullName could not be found in expected-results.json. Kindly add this to the file",
            )
          } else {
            val expectedMaxTiming = (expectedTimings.get("max") ?: 0e1)
            val timingMargin = (expectedTimings.get("margin") ?: 0e1)

            if (median > (expectedMaxTiming + timingMargin)) {
              throw Exception(
                "$fullName test passes the threshold of ${expectedMaxTiming + timingMargin} Ns. The timing is $median Ns",
              )
            } else if (median <= (expectedMaxTiming - timingMargin)) {
              System.out.println(
                "Improvement: Test $fullName took $median vs min of ${expectedMaxTiming - timingMargin}",
              )
            } else {
              System.out.println(
                "Test $fullName took $median vs Range[${expectedMaxTiming - timingMargin} to ${expectedMaxTiming + timingMargin}] Ns",
              )
            }
          }
        }
      }
    } else {
      throw Exception(
        "Results file could not be found in  ${resultsFile.path}. Make sure this file is on the devices. It should be listed in the previous step after adb shell ls /sdcard/Download/",
      )
    }
  }
}

fun JSONObject.getTestName(): String {
  val className = getString("className").substringAfterLast(".")
  val methodName = getString("name").substringAfterLast("_")

  return "$className#$methodName"
}

operator fun JSONArray.iterator(): Iterator<JSONObject> =
  (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
