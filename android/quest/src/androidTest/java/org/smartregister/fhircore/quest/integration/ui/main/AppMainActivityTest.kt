/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration.ui.main

import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.profile.PROFILE_TOP_BAR_TEST_TAG
import org.smartregister.fhircore.quest.ui.register.RegisterFragment.Companion.REGISTER_SCREEN_BOX_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.SCREEN_TITLE
import org.smartregister.fhircore.quest.ui.usersetting.USER_SETTING_ROW_LOGOUT

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class AppMainActivityTest {

  @get:Rule(order = 0)
  val initWorkManager = TestRule { base, _ ->
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build(),
    )
    return@TestRule base
  }

  @get:Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 2) val composeTestRule = createAndroidComposeRule<AppMainActivity>()

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun startDestinationFragmentShouldShowRegisterScreen() {
    composeTestRule.activityRule.scenario.onActivity {
      Assert.assertEquals(
        R.id.registerFragment,
        it.navHostFragment.navController.currentDestination?.id,
      )
    }

    composeTestRule.onNodeWithTag(REGISTER_SCREEN_BOX_TAG).assertIsDisplayed()
  }

  @Test
  fun navigationToUserSettingFragmentShouldShowUserSettingsScreen() {
    composeTestRule.activityRule.scenario.onActivity {
      it.navHostFragment.navController.navigate(R.id.userSettingFragment)
    }

    composeTestRule.onNodeWithTag(USER_SETTING_ROW_LOGOUT).assertExists()
  }

  @Test
  fun navigationToProfileFragmentShouldShowProfileScreen() {
    val patientResourceConfig =
      mockk<ResourceConfig> { every { resource } returns ResourceType.Patient }
    val resourceConfig =
      mockk<FhirResourceConfig> { every { baseResource } returns patientResourceConfig }

    composeTestRule.activityRule.scenario.onActivity {
      it.navHostFragment.navController.navigate(
        R.id.profileFragment,
        bundleOf(
          NavigationArg.PROFILE_ID to "defaultProfile",
          NavigationArg.RESOURCE_CONFIG to resourceConfig,
          NavigationArg.PARAMS to
            arrayOf(
              ActionParameter(
                key = "anyId",
                paramType = ActionParameterType.PARAMDATA,
                value = "anyValue",
              ),
            ),
        ),
      )
    }

    composeTestRule.onNodeWithTag(PROFILE_TOP_BAR_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun navigationToMeasureReportFragmentShouldShowMeasureReportScreen() {
    composeTestRule.activityRule.scenario.onActivity {
      it.navHostFragment.navController.navigate(
        R.id.measureReportFragment,
        bundleOf(
          NavigationArg.REPORT_ID to "serviceDeliveryMeasureReport",
          NavigationArg.RESOURCE_ID to "",
        ),
      )
    }

    composeTestRule.onNodeWithTag(SCREEN_TITLE).assertIsDisplayed()
  }
}
