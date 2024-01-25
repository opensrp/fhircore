import java.io.FileReader
import org.json.JSONArray
import org.json.JSONObject

plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "org.smartregister.opensrp.quest.macrobenchmark"
  compileSdk = 33

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions { jvmTarget = "1.8" }

  defaultConfig {
    minSdk = 24
    targetSdk = 33

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,DEBUGGABLE"
    missingDimensionStrategy("apps", "opensrp")
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It"s signed with a debug key
    // for easy local/CI testing.
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":quest"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
  implementation("androidx.test.ext:junit:1.1.5")
  implementation("androidx.test.espresso:espresso-core:3.4.0")
  implementation("androidx.test.uiautomator:uiautomator:2.2.0")
  implementation("androidx.benchmark:benchmark-macro-junit4:1.1.1")
  implementation("androidx.work:work-testing:2.7.1")
  // implementation("androidx.tracing:tracing:1.2.0-rc01")
}

androidComponents { beforeVariants(selector().all()) { it.enabled = it.buildType == "benchmark" } }

/**
 * This task compares the performance benchmark results to the expected benchmark results and throws
 * an error if the result is past the expected result and margin. A message will also be printed if
 * the performance significantly improves.
 */
task("evaluatePerformanceBenchmarkResults") {
  val expectedPerformanceLimitsFile = project.file("expected-results.json")
  val resultsFile = project.file("benchmark-results.json")

  doLast {
    if (resultsFile.exists()) {
      // Read the expectations file
      val expectedResultsMap: HashMap<String, HashMap<String, Double>> = hashMapOf()

      // This assumes that each test only has
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
        val benchMarksArray: JSONArray = this.getJSONArray("benchmarks")
        for (i in 0 until benchMarksArray.length()) {
          val benchmarkResult = benchMarksArray.getJSONObject(i) as JSONObject
          val fullName = benchmarkResult.getTestName()
          val traceName = benchmarkResult.getJSONObject("metrics").keys().next()
          val timings = benchmarkResult.getJSONObject("metrics").getJSONObject(traceName.toString())

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
