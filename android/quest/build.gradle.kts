import com.android.build.api.variant.FilterConfiguration.FilterType
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
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
  id("io.sentry.android.gradle") version "3.11.1"
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
      isShrinkResources = false
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

    resources.excludes.addAll(
      listOf(
        "META-INF/*.kotlin_module",
      ),
    )
    resources.pickFirsts.addAll(
      listOf(
        "META-INF/services/org.apache.commons.logging.LogFactory",
        "org/apache/commons/logging/commons-logging.properties",
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

  lint {
    checkReleaseBuilds = true
    abortOnError = false
  }

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

    create("kaderJobAids") {
      dimension = "apps"
      applicationIdSuffix = ".jobaids"
      versionNameSuffix = "-kaderJobAids"
      manifestPlaceholders["appLabel"] = "Kader Kesehatan"
    }

    create("bkmChis") {
      dimension = "apps"
      applicationIdSuffix = ".bkmchis"
      versionNameSuffix = "-bkmChis"
      manifestPlaceholders["appLabel"] = "BKM CHIS"
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
    tasks.register("jacocoTestReport${variant.name.replaceFirstChar { it.uppercase() }}")
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
  addTestListener(
    object : TestListener {
      override fun beforeSuite(p0: TestDescriptor?) {}

      override fun afterSuite(p0: TestDescriptor?, p1: TestResult?) {}

      override fun beforeTest(p0: TestDescriptor?) {
        logger.lifecycle("Running test: $p0")
      }

      override fun afterTest(p0: TestDescriptor?, p1: TestResult?) {
        logger.lifecycle("Done executing: $p0")
      }
    },
  )

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
  implementation(libs.androidx.fragment.compose)
  implementation(libs.bundles.cameraX)
  implementation(libs.log4j)

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
  // debugImplementation(libs.leakcanary.android)

  kapt(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  testImplementation(libs.androidx.room.testing)

  implementation(libs.android.database.sqlcipher)

  implementation(libs.zip4j)

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

sentry {
  // Disables or enables debug log output, e.g. for for sentry-cli.
  // Default is disabled.
  debug.set(true)

  // The slug of the Sentry organization to use for uploading proguard mappings/source contexts.
  org.set("""${project.extra["org"]}""")

  // The slug of the Sentry project to use for uploading proguard mappings/source contexts.
  projectName.set("""${project.extra["project"]}""")

  // The authentication token to use for uploading proguard mappings/source contexts.
  // WARNING: Do not expose this token in your build.gradle files, but rather set an environment
  // variable and read it into this property.
  authToken.set("""${project.extra["auth.token"]}""")

  // The url of your Sentry instance. If you're using SAAS (not self hosting) you do not have to
  // set this. If you are self hosting you can set your URL here
  // url = null

  // Disables or enables the handling of Proguard mapping for Sentry.
  // If enabled the plugin will generate a UUID and will take care of
  // uploading the mapping to Sentry. If disabled, all the logic
  // related to proguard mapping will be excluded.
  // Default is enabled.
  includeProguardMapping.set(false)

  // Whether the plugin should attempt to auto-upload the mapping file to Sentry or not.
  // If disabled the plugin will run a dry-run and just generate a UUID.
  // The mapping file has to be uploaded manually via sentry-cli in this case.
  // Default is enabled.
  autoUploadProguardMapping.set(false)

  // Experimental flag to turn on support for GuardSquare's tools integration (Dexguard and
  // External Proguard).
  // If enabled, the plugin will try to consume and upload the mapping file produced by Dexguard
  // and External Proguard.
  // Default is disabled.
  // dexguardEnabled.set(false)

  // Disables or enables the automatic configuration of Native Symbols
  // for Sentry. This executes sentry-cli automatically so
  // you don't need to do it manually.
  // Default is disabled.
  uploadNativeSymbols.set(false)

  // Whether the plugin should attempt to auto-upload the native debug symbols to Sentry or not.
  // If disabled the plugin will run a dry-run.
  // Default is enabled.
  autoUploadNativeSymbols.set(true)

  // Does or doesn't include the source code of native code for Sentry.
  // This executes sentry-cli with the --include-sources param. automatically so
  // you don't need to do it manually.
  // Default is disabled.
  includeNativeSources.set(false)

  // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
  // This enables source context, allowing you to see your source
  // code as part of your stack traces in Sentry.
  includeSourceContext.set(false)

  // Configure additional directories to be included in the source bundle which is used for
  // source context. The directories should be specified relative to the Gradle module/project's
  // root. For example, if you have a custom source set alongside 'main', the parameter would be
  // 'src/custom/java'.
  additionalSourceDirsForSourceContext.set(emptySet())

  // Enable or disable the tracing instrumentation.
  // Does auto instrumentation for specified features through bytecode manipulation.
  // Default is enabled.
  tracingInstrumentation {
    enabled.set(true)

    // Specifies a set of instrumentation features that are eligible for bytecode manipulation.
    // Defaults to all available values of InstrumentationFeature enum class.
    features.set(
      setOf(
        InstrumentationFeature.DATABASE,
        InstrumentationFeature.FILE_IO,
        InstrumentationFeature.OKHTTP,
        InstrumentationFeature.COMPOSE,
      ),
    )

    // Enable or disable logcat instrumentation through bytecode manipulation.
    // Default is enabled.
    logcat {
      enabled.set(true)

      // Specifies a minimum log level for the logcat breadcrumb logging.
      // Defaults to LogcatLevel.WARNING.
      minLevel.set(LogcatLevel.WARNING)
    }

    // The set of glob patterns to exclude from instrumentation. Classes matching any of these
    // patterns in the project's sources and dependencies JARs won't be instrumented by the
    // Sentry Gradle plugin.
    //
    // Don't include the file extension. Filtering is done on compiled classes and
    // the .class suffix isn't included in the pattern matching.
    //
    // Example usage:
    // ```
    // excludes.set(setOf("com/example/donotinstrument/**", "**/*Test"))
    // ```
    //
    // Only supported when using Android Gradle plugin (AGP) version 7.4.0 and above.
    // excludes.set(emptySet())
  }

  // Enable auto-installation of Sentry components (sentry-android SDK and okhttp, timber, fragment
  // and compose integrations).
  // Default is enabled.
  // Only available v3.1.0 and above.
  autoInstallation {
    enabled.set(true)

    // Specifies a version of the sentry-android SDK and fragment, timber and okhttp integrations.
    //
    // This is also useful, when you have the sentry-android SDK already included into a transitive
    // dependency/module and want to
    // align integration versions with it (if it's a direct dependency, the version will be
    // inferred).
    //
    // NOTE: if you have a higher version of the sentry-android SDK or integrations on the
    // classpath, this setting will have no effect
    // as Gradle will resolve it to the latest version.
    //
    // Defaults to the latest published Sentry version.
    sentryVersion.set("7.14.0")
  }

  // Disables or enables dependencies metadata reporting for Sentry.
  // If enabled, the plugin will collect external dependencies and
  // upload them to Sentry as part of events. If disabled, all the logic
  // related to the dependencies metadata report will be excluded.
  //
  // Default is enabled.
  includeDependenciesReport.set(true)

  // Whether the plugin should send telemetry data to Sentry.
  // If disabled the plugin won't send telemetry data.
  // This is auto disabled if running against a self hosted instance of Sentry.
  // Default is enabled.
  //  telemetry.set(true)
}

fun JSONObject.getTestName(): String {
  val className = getString("className").substringAfterLast(".")
  val methodName = getString("name").substringAfterLast("_")

  return "$className#$methodName"
}

operator fun JSONArray.iterator(): Iterator<JSONObject> =
  (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
