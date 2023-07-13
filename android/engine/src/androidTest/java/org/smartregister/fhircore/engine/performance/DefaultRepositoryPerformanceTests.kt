package org.smartregister.fhircore.engine.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import javax.inject.Inject


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 11-07-2023.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DefaultRepositoryPerformanceTests {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Inject lateinit var defaultRepository: DefaultRepository
    @Inject lateinit var registerRepository: RegisterRepository


    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun benchmarkSomeWork() {
        benchmarkRule.measureRepeated {
            registerRepository.retrieveRegisterConfiguration("householdRegister", hashMapOf())
        }
    }
}
