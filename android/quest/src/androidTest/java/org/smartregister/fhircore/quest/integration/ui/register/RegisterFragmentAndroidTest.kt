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

package org.smartregister.fhircore.quest.integration.ui.register

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.test.R as R2
import org.smartregister.fhircore.quest.integration.ui.TestActivity
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.fhircore.quest.ui.register.RegisterFragment

@OptIn(ExperimentalMaterialApi::class)
class RegisterFragmentAndroidTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<AppMainActivity>()

  @Before
  fun setUp() {
    composeTestRule.activityRule.scenario.onActivity {
      val navHostFrag =
        NavHostFragment.create(
          R.navigation.application_nav_graph,
          bundleOf(
            NavigationArg.SCREEN_TITLE to "Test",
            NavigationArg.REGISTER_ID to "test",
          ),
        )
      it.supportFragmentManager
        .beginTransaction()
        .replace(R2.id.container_holder, navHostFrag)
        .setPrimaryNavigationFragment(navHostFrag)
        .commit()
    }
  }

  @Test
  fun shouldDisplayRegisterScreen() {
    composeTestRule.onNodeWithTag(RegisterFragment.REGISTER_SCREEN_TEST_TAG).assertIsDisplayed()
  }
}
