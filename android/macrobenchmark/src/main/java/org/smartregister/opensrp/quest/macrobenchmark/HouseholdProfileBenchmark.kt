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
class HouseholdProfileBenchmark {

  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @OptIn(ExperimentalMetricApi::class)
  @Test
  fun benchmarkOpenningProfile() {
    val traceName = "Load household profile"
    val familyName = "Mwanake Family"
    benchmarkRule.measureRepeated(
      packageName = "org.smartregister.opensrp",
      metrics = listOf(StartupTimingMetric(), TraceSectionMetric(traceName)),
      iterations = 1,
      startupMode = StartupMode.COLD,
      setupBlock = setupDevice(familyName),
    ) {
      device.findObject(By.text(familyName)).click()
      Trace.beginSection(traceName)

      checkItemByText(device, "King Best Mwanake, M, 45y")

      Trace.endSection()
    }
  }

  fun setupDevice(targetFamilyName: String): MacrobenchmarkScope.() -> Unit = {
    val intent =
      Intent()
        .setComponent(
          ComponentName(
            "org.smartregister.opensrp",
            "org.smartregister.fhircore.quest.ui.main.TestAppMainActivity",
          ),
        )
    startActivityAndWait(intent)

    val toyotaFamilySelector = By.text("Toyota Family")

    if (!device.wait(Until.hasObject(toyotaFamilySelector), 20_000)) {
      throw Exception(
        "Toyota Family could not be found on the UI because the household register could not load within 20 secs or the default register has changed!!",
      )
    }

    scrollToItem(targetFamilyName)
  }

  private fun MacrobenchmarkScope.scrollToItem(label: String, contains: Boolean = false) {
    val selector = if (contains) By.textContains(label) else By.text(label)
    while (!device.wait(Until.hasObject(selector), 1_000)) {
      device.wait(Until.hasObject(By.scrollable(true)), 10_000)
      device.findObject(By.scrollable(true)).fling(Direction.DOWN)
    }
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
