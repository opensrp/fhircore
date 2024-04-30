/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.opensrp.quest.macrobenchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.tracing.Trace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HouseholdRegisterMacrobenchmark {

  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  val iterations = 1

  @OptIn(ExperimentalMetricApi::class)
  @Test
  fun benchmarkPage1() {
    val traceName = "Load Household register page 1"
    benchmarkRule.measureRepeated(
      packageName = "org.smartregister.opensrp",
      metrics = listOf(StartupTimingMetric(), TraceSectionMetric(traceName)),
      iterations = iterations,
      startupMode = StartupMode.COLD,
      setupBlock = {
        pressHome()
        goToPage()
      },
    ) {
      Trace.beginSection(traceName)

      val startWait = System.currentTimeMillis()
      checkItemByText(device, "Toyota Family")
      val stopWait = System.currentTimeMillis()
      System.out.println("Time taken = ${(stopWait - startWait) / 1000} secs")

      checkItemByText(device, "Wambu Family")
      checkItemByText(device, "Waziti Family")
      checkItemByText(device, "Memonons Family")
      checkItemByText(device, "Abdul July Family")
      checkItemByText(device, "FifthJuly Family")
      checkItemByText(device, "All households")

      Trace.endSection()
    }
  }

  @Test
  fun benchmarkPage2() {
    benchmarkPage(
      2,
      "Mananas Family",
      hashMapOf("firstFamily" to "Toyota Family", "lastFamily" to "Kabaka Family"),
    )
  }

  @Test
  fun benchmarkPage3() {
    benchmarkPage(
      3,
      "voldemart Family",
      hashMapOf("firstFamily" to "Toyota Family", "lastFamily" to "Kabaka Family"),
      hashMapOf("firstFamily" to "Mananas Family", "lastFamily" to "Dinar Family"),
    )
  }

  @Test
  fun benchmarkPage4() {
    benchmarkPage(
      4,
      "New Wash Family",
      hashMapOf("firstFamily" to "Toyota Family", "lastFamily" to "Kabaka Family"),
      hashMapOf("firstFamily" to "Mananas Family", "lastFamily" to "Dinar Family"),
      hashMapOf("firstFamily" to "voldemart Family", "lastFamily" to "Melbourne Family"),
    )
  }

  @Test
  fun benchmarkPage5() {
    benchmarkPage(
      5,
      "Josphat Family",
      hashMapOf("firstFamily" to "Toyota Family", "lastFamily" to "Kabaka Family"),
      hashMapOf("firstFamily" to "Mananas Family", "lastFamily" to "Dinar Family"),
      hashMapOf("firstFamily" to "voldemart Family", "lastFamily" to "Melbourne Family"),
      hashMapOf("firstFamily" to "New Wash Family", "lastFamily" to "Diamond2 Family"),
    )
  }

  @OptIn(ExperimentalMetricApi::class)
  fun benchmarkPage(pageNo: Int, firstFamily: String, vararg pages: HashMap<String, String>) {
    val traceName = "Load Household register page no $pageNo "
    benchmarkRule.measureRepeated(
      packageName = "org.smartregister.opensrp",
      metrics = listOf(StartupTimingMetric(), TraceSectionMetric(traceName)),
      iterations = iterations,
      startupMode = StartupMode.COLD,
      setupBlock = {
        pressHome()
        goToPage(*pages)
      },
    ) {
      Trace.beginSection(traceName)

      val startWait = System.currentTimeMillis()
      checkItemByText(device, firstFamily)
      val stopWait = System.currentTimeMillis()
      System.out.println("Time taken = ${(stopWait - startWait) / 1000} secs")

      Trace.endSection()
    }
  }

  fun MacrobenchmarkScope.goToPage(vararg pages: HashMap<String, String>) {
    val intent =
      Intent()
        .setComponent(
          ComponentName(
            "org.smartregister.opensrp",
            "org.smartregister.fhircore.quest.ui.main.TestAppMainActivity",
          ),
        )
    startActivityAndWait(intent)

    pages.forEachIndexed { index, page ->
      val pageNo = index + 1
      log("Waiting for page $pageNo to load")

      checkItemByText(device, page["firstFamily"]!!)

      log("Loaded page $pageNo")

      scrollToItem(page["lastFamily"]!!)

      log("About to go to page $pageNo")
      device.findObject(By.text("NEXT")).click()
    }
  }

  private fun MacrobenchmarkScope.scrollToItem(label: String, contains: Boolean = false) {
    val selector = if (contains) By.textContains(label) else By.text(label)
    while (!device.wait(Until.hasObject(selector), 1_000)) {
      device.wait(Until.hasObject(By.scrollable(true)), 10_000)
      device.findObject(By.scrollable(true)).fling(Direction.DOWN)
    }
  }

  private fun log(printMsg: String) {
    System.out.println(printMsg)
  }

  fun checkItemByText(device: UiDevice, label: String) {
    val result = device.wait(Until.hasObject(By.text(label)), 20_000)
    if (!result) {
      throw Exception(
        "$label could not be found on the UI because the household register could not load within 5 secs or the default register has changed!!",
      )
    }
  }
}
