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

package org.smartregister.fhircore.quest.ui.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.compose.material.ExperimentalMaterialApi
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.State
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.bottomsheet.RegisterBottomSheetFragment
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
@OptIn(ExperimentalMaterialApi::class)
class AppMainViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var gson: Gson

  private val accountAuthenticator: AccountAuthenticator = mockk(relaxed = true)

  private val syncBroadcaster: SyncBroadcaster = mockk(relaxed = true)

  private val secureSharedPreference: SecureSharedPreference = mockk()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val registerRepository: RegisterRepository = mockk()

  private val application: Context = ApplicationProvider.getApplicationContext()

  private lateinit var appMainViewModel: AppMainViewModel

  private val navController = mockk<NavController>(relaxUnitFun = true)

  @Before
  fun setUp() {
    hiltRule.inject()

    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    appMainViewModel =
      spyk(
        AppMainViewModel(
          accountAuthenticator = accountAuthenticator,
          syncBroadcaster = syncBroadcaster,
          secureSharedPreference = secureSharedPreference,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          registerRepository = registerRepository,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider
        )
      )

    runBlocking { configurationRegistry.loadConfigurations("app/debug", application) }
  }

  @Test
  fun testOnEventLogout() {
    val appMainEvent = AppMainEvent.Logout

    appMainViewModel.onEvent(appMainEvent)

    verify { accountAuthenticator.logout() }
  }

  @Test
  fun testOnEventSwitchLanguage() {
    val appMainEvent =
      AppMainEvent.SwitchLanguage(
        Language("en", "English"),
        mockkClass(Activity::class, relaxed = true)
      )

    appMainViewModel.onEvent(appMainEvent)

    Assert.assertEquals("en", sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, ""))
  }

  @Test
  fun testOnEventSyncData() {
    val appMainEvent = AppMainEvent.SyncData
    appMainViewModel.onEvent(appMainEvent)

    verify { syncBroadcaster.runSync() }
    verify { appMainViewModel.retrieveAppMainUiState() }
  }

  @Test
  fun testOnEventUpdateSyncStates() {
    val stateInProgress = mockk<State.InProgress>()
    appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(stateInProgress, "Some timestamp"))
    Assert.assertEquals("Some timestamp", appMainViewModel.appMainUiState.value.lastSyncTime)

    // Simulate sync state Finished
    val timestamp = OffsetDateTime.now()
    val success = spyk(Result.Success())
    every { success.timestamp } returns timestamp
    val stateFinished = mockk<State.Finished>()
    every { stateFinished.result } returns success

    appMainViewModel.onEvent(AppMainEvent.UpdateSyncState(stateFinished, "Some timestamp"))
    Assert.assertEquals(
      appMainViewModel.formatLastSyncTimestamp(timestamp),
      sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
    )
    verify { appMainViewModel.retrieveAppMainUiState() }
  }

  @Test
  fun testOnEventOpenProfile() {
    appMainViewModel.onEvent(
      AppMainEvent.OpenProfile(
        navController = navController,
        profileId = "profileId",
        resourceId = "resourceId"
      )
    )

    val intSlot = slot<Int>()
    val bundleSlot = slot<Bundle>()
    verify { navController.navigate(capture(intSlot), capture(bundleSlot)) }

    Assert.assertEquals(MainNavigationScreen.Profile.route, intSlot.captured)
    Assert.assertEquals(2, bundleSlot.captured.size())
    Assert.assertEquals("profileId", bundleSlot.captured.getString(NavigationArg.PROFILE_ID))
    Assert.assertEquals("resourceId", bundleSlot.captured.getString(NavigationArg.RESOURCE_ID))
  }

  @Test
  fun testOnEventTriggerWorkflow() {
    val action =
      spyk(
        listOf(
          ActionConfig(
            trigger = ActionTrigger.ON_CLICK,
            workflow = ApplicationWorkflow.LAUNCH_REPORT
          )
        )
      )
    val navMenu = spyk(NavigationMenuConfig(id = "menuId", display = "Menu Item", actions = action))
    appMainViewModel.onEvent(
      AppMainEvent.TriggerWorkflow(navController = navController, navMenu = navMenu)
    )
    // We have triggered workflow for launching report
    val intSlot = slot<Int>()
    verify { navController.navigate(capture(intSlot)) }
    Assert.assertEquals(MainNavigationScreen.Reports.route, intSlot.captured)
  }

  @Test
  fun testOnEventOpenRegistersBottomSheet() {
    val controller = Robolectric.buildActivity(HiltActivityForTest::class.java).create().resume()
    val activityForTest = controller.get()
    every { navController.context } returns activityForTest
    appMainViewModel.onEvent(
      AppMainEvent.OpenRegistersBottomSheet(
        navController = navController,
        registersList = emptyList()
      )
    )

    // Assert fragment that was launched is RegisterBottomSheetFragment
    activityForTest.supportFragmentManager.executePendingTransactions()
    val fragments = activityForTest.supportFragmentManager.fragments
    Assert.assertEquals(1, fragments.size)
    Assert.assertTrue(fragments.first() is RegisterBottomSheetFragment)
    // Destroy the activity
    controller.destroy()
  }

  @Test
  fun onRefreshAuthToken() {
    appMainViewModel.onEvent(AppMainEvent.RefreshAuthToken)

    verify { accountAuthenticator.loadRefreshedSessionAccount(any()) }
  }
}
