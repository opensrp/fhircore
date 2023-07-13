package org.smartregister.fhircore.quest.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.quest.QuestTestRunner
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import timber.log.Timber
import javax.inject.Inject


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 11-07-2023.
 */
//@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DefaultRepositoryPerformanceTests {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Inject lateinit var defaultRepository: DefaultRepository
    @Inject lateinit var registerRepository: RegisterRepository


    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        if (InstrumentationRegistry.getInstrumentation().context.externalCacheDir == null ) {
            Timber.e("Instrumentation registry is null")
        } else {
            Timber.e("Instrumentation registry is NOT NULL")
        }

        runBlocking {
            defaultRepository.configurationRegistry.loadConfigurations("app/debug", InstrumentationRegistry.getInstrumentation().targetContext) { loadConfigSuccessful ->

                /*if (loadConfigSuccessful) {
                    sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, thisAppId)
                    context.getActivity()?.launchActivityWithNoBackStackHistory<LoginActivity>()
                } else {
                    _error.postValue(context.getString(R.string.application_not_supported, appId.value))
                }*/
            }
        }

        // Plant Timber tree to print in the console

        Timber.plant(Timber.DebugTree())
    }

    @Test
    fun benchmarkSomeWork() {
        benchmarkRule.measureRepeated {
            //registerRepository.retrieveRegisterConfiguration("householdRegister", hashMapOf())

            runBlocking {
                val repoData = registerRepository.loadRegisterData(0, "householdRegister")
                System.out.println("Records fetched ${repoData.size}")
            }
        }
    }
}
