/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.appsetting

// import android.app.Activity
// import android.content.Context
// import android.widget.Toast
// import androidx.test.core.app.ApplicationProvider
// import dagger.hilt.android.testing.BindValue
// import dagger.hilt.android.testing.HiltAndroidRule
// import dagger.hilt.android.testing.HiltAndroidTest
// import io.mockk.coEvery
// import io.mockk.every
// import io.mockk.mockk
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.test.runTest
// import org.junit.Assert
// import org.junit.Before
// import org.junit.Ignore
// import org.junit.Rule
// import org.junit.Test
// import org.robolectric.Robolectric
// import org.robolectric.android.controller.ActivityController
// import org.robolectric.shadows.ShadowToast
// import org.smartregister.fhircore.engine.R
// import org.smartregister.fhircore.engine.app.fakes.Faker
// import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
// import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
// import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
// import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
// import org.smartregister.fhircore.engine.util.IS_LOGGED_IN
// import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
//
// @ExperimentalCoroutinesApi
// @HiltAndroidTest
// class AppSettingActivityTest : ActivityRobolectricTest() {
//
//  @get:Rule val hiltRule = HiltAndroidRule(this)
//
//  lateinit var activityController: ActivityController<AppSettingActivity>
//  lateinit var appSettingActivity: AppSettingActivity
//
//  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
//  @BindValue
//  var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository = mockk())
//
//  val testAppId = "default"
//
//  @Before
//  fun setUp() {
//    hiltRule.inject()
//    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
//    activityController = Robolectric.buildActivity(AppSettingActivity::class.java)
//    appSettingActivity = activityController.get()
//    activityController.create().resume()
//  }
//
//  @Test
//  fun testThatConfigsAreLoadedCorrectlyAndActivityIsFinished() {
//    runTest {
//      appSettingActivity.appSettingViewModel.run {
//        onApplicationIdChanged(testAppId)
//        loadConfigurations(true)
//      }
//    }
//
//    val workflowPointsMap = appSettingActivity.configurationRegistry.workflowPointsMap
//    Assert.assertTrue(workflowPointsMap.isNotEmpty())
//    Assert.assertTrue(workflowPointsMap.containsKey("default|application"))
//    val configuration =
//      appSettingActivity.configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
//        AppConfigClassification.APPLICATION
//      )
//    Assert.assertEquals(testAppId, configuration.appId)
//    Assert.assertEquals("application", configuration.classification)
//    Assert.assertEquals("DefaultAppTheme", configuration.theme)
//    Assert.assertTrue(configuration.languages.containsAll(listOf("en", "sw")))
//    // TODO Assert.assertTrue(appSettingActivity.isFinishing)
//  }
//
//  @Test
//  @Ignore
//  fun testThatConfigsAreNotLoadedAndToastNotificationDisplayed() = runTest {
//    coEvery { configurationRegistry.repository.searchCompositionByIdentifier("wrongAppId") }
// returns
//      null
//
//    appSettingActivity.appSettingViewModel.run {
//      onApplicationIdChanged("wrongAppId")
//      loadConfigurations(true)
//    }
//    val latestToast = ShadowToast.getLatestToast()
//    Assert.assertEquals(Toast.LENGTH_LONG, latestToast.duration)
//  }
//
//  @Test
//  fun testLocalConfig() {
//    appSettingActivity.appSettingViewModel.run {
//      onApplicationIdChanged("$testAppId/debug")
//      loadConfigurations(true)
//    }
//
//    Assert.assertTrue(appSettingActivity.appSettingViewModel.hasDebugSuffix() == true)
//    Assert.assertEquals("default/debug", appSettingActivity.appSettingViewModel.appId.value)
//    Assert.assertEquals(9, appSettingActivity.configurationRegistry.workflowPointsMap.size)
//
//    val workflows = appSettingActivity.configurationRegistry.workflowPointsMap
//    Assert.assertTrue(workflows.containsKey("default|application"))
//    Assert.assertTrue(workflows.containsKey("default|login"))
//    Assert.assertTrue(workflows.containsKey("default|app_feature"))
//    Assert.assertTrue(workflows.containsKey("default|patient_register"))
//    Assert.assertTrue(workflows.containsKey("default|patient_task_register"))
//    Assert.assertTrue(workflows.containsKey("default|pin"))
//    Assert.assertTrue(workflows.containsKey("default|patient_details_view"))
//    Assert.assertTrue(workflows.containsKey("default|result_details_navigation"))
//    Assert.assertTrue(workflows.containsKey("default|sync"))
//  }
//
//  @Test
//  fun testLocalAppId() {
//    every { sharedPreferencesHelper.read(IS_LOGGED_IN, false) } returns false
//    every { sharedPreferencesHelper.read(APP_ID_CONFIG, null) } returns "quest "
//    activityController.recreate()
//  }
//
//  override fun getActivity(): Activity = appSettingActivity
// }
