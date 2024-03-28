package org.smartregister.opensrp.quest.macrobenchmark.startup

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.smartregister.opensrp.quest.macrobenchmark.constants.DEFAULT_ITERATIONS
import org.smartregister.opensrp.quest.macrobenchmark.constants.TARGET_PACKAGE

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppSettingsMacroBenchMark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()


    @Test
    fun launchApp() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            iterations = DEFAULT_ITERATIONS,
        ) {
            startActivityAndWait()
        }
    }
}