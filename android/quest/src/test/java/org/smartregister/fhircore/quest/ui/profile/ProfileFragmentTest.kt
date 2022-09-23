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

package org.smartregister.fhircore.quest.ui.profile

import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity

@OptIn(ExperimentalMaterialApi::class)
@HiltAndroidTest
class ProfileFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue val registerRepository: RegisterRepository = mockk(relaxUnitFun = true)

  private val activityController = Robolectric.buildActivity(AppMainActivity::class.java)

  private lateinit var navController: TestNavHostController

  private val patient = Faker.buildPatient()

  private lateinit var mainActivity: AppMainActivity

  lateinit var profileFragment: ProfileFragment

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    // Initialize WorkManager for instrumentation tests.
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val config =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

    profileFragment =
      ProfileFragment().apply {
        arguments =
          bundleOf(
            NavigationArg.PROFILE_ID to "defaultProfile",
            NavigationArg.RESOURCE_ID to patient.id
          )
      }
    activityController.create().resume()
    mainActivity = activityController.get()
    navController =
      TestNavHostController(mainActivity).apply { setGraph(R.navigation.application_nav_graph) }

    // Simulate the returned value of loadProfile
    coEvery { registerRepository.loadProfileData(any(), any()) } returns
      ResourceData(
        baseResource = patient,
        relatedResourcesMap = emptyMap(),
        computedValuesMap =
          mapOf("patientName" to patient.name, "patientId" to patient.identifierFirstRep)
      )
  }

  @Test
  fun testProfileFragmentCreation() {
    Navigation.setViewNavController(mainActivity.navHostFragment.requireView(), navController)
    mainActivity.supportFragmentManager.run {
      commitNow { add(profileFragment, ProfileFragment::class.java.simpleName) }
      executePendingTransactions()
    }
    Assert.assertTrue(profileFragment.view is ComposeView)
    activityController.destroy()
  }
}
