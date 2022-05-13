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

package org.smartregister.fhircore.engine.ui.register

import android.Manifest
import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.time.OffsetDateTime
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowIntent
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.app.fakes.FakeModel
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.auth.TokenManagerService
import org.smartregister.fhircore.engine.configuration.ConfigClassification
import org.smartregister.fhircore.engine.configuration.view.NavigationOption
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asString
import retrofit2.HttpException

@HiltAndroidTest
class BaseRegisterActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @BindValue var tokenManagerService: TokenManagerService = mockk()

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()
  @BindValue val accountAuthenticator = mockk<AccountAuthenticator>()

  val defaultRepository: DefaultRepository = mockk()
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository)

  private lateinit var testRegisterActivityController: ActivityController<TestRegisterActivity>

  private lateinit var testRegisterActivity: TestRegisterActivity

  @Before
  fun setUp() {
    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } returns ""
    every { sharedPreferencesHelper.write(any(), any<String>()) } returns Unit
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"
    every { secureSharedPreference.retrieveCredentials() } returns FakeModel.authCredentials
    every { secureSharedPreference.deleteCredentials() } returns Unit

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }

    testRegisterActivityController = Robolectric.buildActivity(TestRegisterActivity::class.java)
    testRegisterActivity = testRegisterActivityController.get()
    testRegisterActivityController.create().resume()
  }

  override fun tearDown() {
    // Reset syncBroadcaster
    super.tearDown()
  }

  override fun getActivity(): Activity = testRegisterActivity

  @Test
  fun testViewSetup() {

    // Main Fragment is displayed
    Assert.assertTrue(testRegisterActivity.supportFragmentManager.fragments.isNotEmpty())
    val findFragmentByTag: Fragment? =
      testRegisterActivity.supportFragmentManager.findFragmentByTag(TestFragment.TAG + 1)
    Assert.assertNotNull(findFragmentByTag)

    // ViewBindings correctly setup
    Assert.assertNotNull(testRegisterActivity.drawerMenuHeaderBinding)
    Assert.assertNotNull(testRegisterActivity.registerActivityBinding)

    // Side Menu has 1 item
    Assert.assertEquals(1, testRegisterActivity.sideMenuOptions().size)

    // Register list contains 1 item
    Assert.assertTrue(testRegisterActivity.registersList().isNotEmpty())

    // Support fragment contains 2 fragments
    val supportedFragments = testRegisterActivity.supportedFragments()
    Assert.assertEquals(2, supportedFragments.size)
    Assert.assertTrue(supportedFragments.containsKey(TestFragment.TAG + 1))
    Assert.assertTrue(supportedFragments.containsKey(TestFragment.TAG + 2))

    val config = testRegisterActivity.registerViewModel.registerViewConfiguration.value!!

    // Bottom navigation should not contains any menu option
    Assert.assertTrue(testRegisterActivity.bottomNavigationMenuOptions(config).isEmpty())

    config.bottomNavigationOptions =
      listOf(
        NavigationOption(
          id = "profile",
          title = getString(R.string.profile),
          icon = "ic_user",
          mockk()
        )
      )

    // Bottom navigation contains one menu option
    Assert.assertTrue(testRegisterActivity.bottomNavigationMenuOptions(config).isNotEmpty())
  }

  @Test
  fun testConfigureView_with_sideMenu() {
    val registerViewConfiguration =
      testRegisterActivity.registerViewConfigurationOf(
        appId = "appId",
        classification = "patient_register",
        appTitle = "Covax",
        filterText = "Show overdue",
        searchBarHint = "Search name or ID",
        newClientButtonText = "Register new client",
        newClientButtonStyle = "",
        showSearchBar = true,
        showFilter = true,
        showScanQRCode = true,
        showNewClientButton = true,
        showSideMenu = true,
        showBottomMenu = false,
        registrationForm = "patient-registration"
      )
    testRegisterActivity.configureViews(registerViewConfiguration)
    Assert.assertEquals(
      registerViewConfiguration.appTitle,
      testRegisterActivity.drawerMenuHeaderBinding.tvNavHeader.text.toString()
    )

    val registerActivityBinding = testRegisterActivity.registerActivityBinding

    // SideMenu or DrawerLayout is visible
    Assert.assertEquals(View.VISIBLE, registerActivityBinding.drawerLayout.visibility)

    // DrawerMenu button is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.btnDrawerMenu.visibility
    )

    // TopToolbarSection is visible
    Assert.assertEquals(
      View.GONE,
      registerActivityBinding.toolbarLayout.topToolbarSection.visibility
    )

    // MiddleToolbarSection is gone
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.middleToolbarSection.visibility
    )

    // New button is visible, text also updated
    Assert.assertEquals(
      registerViewConfiguration.newClientButtonText,
      registerActivityBinding.btnRegisterNewClient.text
    )

    // Search bar is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.editTextSearch.visibility
    )

    // Due button is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.btnShowOverdue.visibility
    )

    // Scan QR Code button is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.btnScanBarcode.visibility
    )

    // BottomNavigation is gone
    Assert.assertEquals(View.GONE, registerActivityBinding.bottomNavView.visibility)
  }

  @Test
  fun testConfigureView_without_sideMenu() {
    val registerViewConfiguration =
      testRegisterActivity.registerViewConfigurationOf(
        appId = "appId",
        classification = "patient_register",
        appTitle = "Covax",
        filterText = "Show overdue",
        searchBarHint = "Search name or ID",
        newClientButtonText = "Register new client",
        newClientButtonStyle = "",
        showSearchBar = true,
        showFilter = true,
        showScanQRCode = true,
        showNewClientButton = true,
        showSideMenu = false,
        showBottomMenu = false,
        registrationForm = "patient-registration"
      )
    testRegisterActivity.configureViews(registerViewConfiguration)
    Assert.assertEquals(
      registerViewConfiguration.appTitle,
      testRegisterActivity.drawerMenuHeaderBinding.tvNavHeader.text.toString()
    )

    val registerActivityBinding = testRegisterActivity.registerActivityBinding

    // SideMenu or DrawerLayout is visible
    Assert.assertEquals(View.VISIBLE, registerActivityBinding.drawerLayout.visibility)

    // DrawerMenu button is gone
    Assert.assertEquals(View.GONE, registerActivityBinding.toolbarLayout.btnDrawerMenu.visibility)

    // TopToolbarSection is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.topToolbarSection.visibility
    )

    // MiddleToolbarSection is gone
    Assert.assertEquals(
      View.GONE,
      registerActivityBinding.toolbarLayout.middleToolbarSection.visibility
    )

    // New button is visible, text also updated
    Assert.assertEquals(
      registerViewConfiguration.newClientButtonText,
      registerActivityBinding.btnRegisterNewClient.text
    )

    // Search bar is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.editTextSearch.visibility
    )

    // Due button is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.btnShowOverdue.visibility
    )

    // Scan QR Code button is visible
    Assert.assertEquals(
      View.VISIBLE,
      registerActivityBinding.toolbarLayout.btnScanBarcode.visibility
    )

    // BottomNavigation is gone
    Assert.assertEquals(View.GONE, registerActivityBinding.bottomNavView.visibility)
  }

  @Test
  fun testDueButton_onClick_updateFilterValue_toTrue() {
    testRegisterActivity.registerViewConfigurationOf(
      appId = "appId",
      classification = "patient_register",
      appTitle = "Covax",
      filterText = "Show overdue",
      searchBarHint = "Search name or ID",
      newClientButtonText = "Register new client",
      newClientButtonStyle = "",
      showSearchBar = true,
      showFilter = true,
      showScanQRCode = true,
      showNewClientButton = true,
      showSideMenu = true,
      showBottomMenu = false,
      registrationForm = "patient-registration"
    )

    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    val btnShowOverdue = registerActivityBinding.toolbarLayout.btnShowOverdue

    // Due button is visible
    Assert.assertEquals(View.VISIBLE, btnShowOverdue.visibility)

    btnShowOverdue.performClick()

    // filter is true
    Assert.assertTrue(testRegisterActivity.registerViewModel.filterValue.value!!.second as Boolean)
  }

  @Test
  fun testDueButton_onClick_updateFilterValue_toNull() {
    testRegisterActivity.registerViewConfigurationOf(
      appId = "appId",
      classification = "patient_register",
      appTitle = "Covax",
      filterText = "Show overdue",
      searchBarHint = "Search name or ID",
      newClientButtonText = "Register new client",
      newClientButtonStyle = "",
      showSearchBar = true,
      showFilter = true,
      showScanQRCode = true,
      showNewClientButton = true,
      showSideMenu = true,
      showBottomMenu = false,
      registrationForm = "patient-registration"
    )

    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    val btnShowOverdue = registerActivityBinding.toolbarLayout.btnShowOverdue

    // Due Button is visible
    Assert.assertEquals(View.VISIBLE, btnShowOverdue.visibility)

    btnShowOverdue.isChecked = true

    btnShowOverdue.performClick()

    // filter is null
    Assert.assertNull(testRegisterActivity.registerViewModel.filterValue.value!!.second)
  }

  @Test
  fun testOnSync_with_syncStatus_started() {
    // Status Sync Started
    testRegisterActivity.onSync(State.Started)
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    Assert.assertEquals(View.VISIBLE, registerActivityBinding.progressSync.visibility)
    Assert.assertEquals(
      testRegisterActivity.getString(R.string.syncing_in_progress),
      registerActivityBinding.tvLastSyncTimestamp.text.toString()
    )
    Assert.assertNull(registerActivityBinding.containerProgressSync.background)
    Assert.assertFalse(registerActivityBinding.containerProgressSync.hasOnClickListeners())
  }

  @Test
  fun testSyncButton_onClick_should_showProgressBar() {
    testRegisterActivity.registerActivityBinding.containerProgressSync.performClick()
    Assert.assertEquals(
      View.VISIBLE,
      testRegisterActivity.registerActivityBinding.progressSync.visibility
    )
    testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
  }

  @Test
  fun testOnSync_with_syncStatus_inProgress() {
    // Status Sync InProgress
    testRegisterActivity.onSync(State.InProgress(ResourceType.Patient))
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    Assert.assertEquals(View.VISIBLE, registerActivityBinding.progressSync.visibility)
    Assert.assertEquals(
      testRegisterActivity.getString(R.string.syncing_in_progress),
      registerActivityBinding.tvLastSyncTimestamp.text.toString()
    )
    Assert.assertNull(registerActivityBinding.containerProgressSync.background)
    Assert.assertFalse(registerActivityBinding.containerProgressSync.hasOnClickListeners())
  }

  @Test
  fun testOnSync_with_syncStatus_finished() {
    // Status Sync Finished
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    val result = spyk(Result.Success())
    val currentDateTime = OffsetDateTime.now()
    every { result.timestamp } returns currentDateTime
    every { sharedPreferencesHelper.read(any(), any<String>()) } answers
      {
        if (firstArg<String>() == LAST_SYNC_TIMESTAMP) {
          currentDateTime.asString()
        } else {
          ""
        }
      }

    testRegisterActivity.onSync(State.Finished(result))
    Assert.assertEquals(View.GONE, registerActivityBinding.progressSync.visibility)
    Assert.assertNotNull(registerActivityBinding.containerProgressSync.background)
    Assert.assertTrue(registerActivityBinding.containerProgressSync.hasOnClickListeners())
    // Shared preference saved with last sync timestamp
    Assert.assertEquals(
      currentDateTime.asString(),
      testRegisterActivity.registerViewModel.sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null)
    )
  }

  @Test
  fun testOnSync_with_syncStatus_failed() {
    // Status Sync Failed
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    val result =
      spyk(
        Result.Error(
          listOf(ResourceSyncException(ResourceType.Patient, Exception("I am a bad exception")))
        )
      )
    val lastDateTimestamp = OffsetDateTime.now()
    every { result.timestamp } returns lastDateTimestamp
    every { sharedPreferencesHelper.read(any(), any<String>()) } answers
      {
        if (firstArg<String>() == LAST_SYNC_TIMESTAMP) {
          lastDateTimestamp.asString()
        } else {
          ""
        }
      }
    testRegisterActivity.onSync(State.Failed(result))
    Assert.assertEquals(View.GONE, registerActivityBinding.progressSync.visibility)
    Assert.assertNotNull(registerActivityBinding.containerProgressSync.background)
    Assert.assertTrue(registerActivityBinding.containerProgressSync.hasOnClickListeners())
    Assert.assertEquals(
      lastDateTimestamp.asString(),
      testRegisterActivity.registerViewModel.sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null)
    )
  }

  @Test
  fun testOnSync_with_syncStatus_glitch() {
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    testRegisterActivity.onSync(
      State.Glitch(
        listOf(ResourceSyncException(ResourceType.Patient, Exception("I am a bad exception")))
      )
    )
    Assert.assertEquals(View.GONE, registerActivityBinding.progressSync.visibility)
    val syncStatus =
      testRegisterActivity.registerViewModel.sharedPreferencesHelper.read(
        LAST_SYNC_TIMESTAMP,
        testRegisterActivity.getString(R.string.syncing_retry)
      )
    Assert.assertEquals(syncStatus, registerActivityBinding.tvLastSyncTimestamp.text.toString())
    Assert.assertNotNull(registerActivityBinding.containerProgressSync.background)
    Assert.assertTrue(registerActivityBinding.containerProgressSync.hasOnClickListeners())
  }

  @Test
  fun testOnNavigation_selectLanguage_onItemClicked_should_showDialog() {
    val languageMenuItem = RoboMenuItem(R.id.menu_item_language)
    testRegisterActivity.onNavigationItemSelected(languageMenuItem)
    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    dialog.clickOnItem(0)

    verify(exactly = 1) { sharedPreferencesHelper.write(SharedPreferencesHelper.LANG, "en") }

    Assert.assertEquals(
      testRegisterActivity.getString(R.string.select_language),
      dialog.title,
    )
    Assert.assertEquals(R.drawable.ic_outline_language_black, dialog.iconId)
  }

  @Test
  fun testOnNavigation_logout_onItemClicked_should_finishActivity() {
    every { tokenManagerService.getActiveAccount() } returns Account("abc", "type")
    every { tokenManagerService.isTokenActive(any()) } returns false
    every { accountAuthenticator.logout() } just runs

    val logoutMenuItem = RoboMenuItem(R.id.menu_item_logout)
    testRegisterActivity.onNavigationItemSelected(logoutMenuItem)
    Assert.assertFalse(
      testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
    )
    verify(exactly = 1) { accountAuthenticator.logout() }
  }

  @Test
  fun testOnNavigation_client_onItemClicked_shouldCloseDrawer() {
    val clientsMenuItem = RoboMenuItem(R.id.menu_item_clients)
    testRegisterActivity.onNavigationItemSelected(clientsMenuItem)
    Assert.assertFalse(
      testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
    )
  }

  @Test
  fun testActivity_destroy() {
    testRegisterActivityController.pause().stop().destroy()
  }

  @Test(expected = IllegalArgumentException::class)
  fun testLastSyncTimestamp_isWrongDate_throws_illegalArgumentException() {
    testRegisterActivity.registerViewModel.lastSyncTimestamp.value = "2021-12-15"
    Assert.assertTrue(
      testRegisterActivity.registerActivityBinding.tvLastSyncTimestamp.text.isEmpty()
    )
  }

  @Test
  fun testRegisterClient_should_launchQuestionnaireActivity() {
    testRegisterActivity.registerClient(null)
    val startedIntent: Intent = Shadows.shadowOf(testRegisterActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = Shadows.shadowOf(startedIntent)
    Assert.assertEquals(QuestionnaireActivity::class.java, shadowIntent.intentClass)
  }

  @Test
  fun testSwitch_to_nonRegisterFragment() {
    testRegisterActivity.switchFragment(
      tag = TestFragment.TAG + 2,
      isRegisterFragment = false,
      isFilterVisible = false,
      toolbarTitle = null
    )
    val registerActivityBinding = testRegisterActivity.registerActivityBinding

    Assert.assertFalse(
      registerActivityBinding.toolbarLayout.registerFilterTextview.hasOnClickListeners()
    )

    Assert.assertEquals(
      testRegisterActivity.getString(R.string.clients),
      registerActivityBinding.toolbarLayout.registerFilterTextview.text.toString()
    )
  }

  @Test
  fun testSwitch_to_nonRegisterFragment_with_newTitle() {
    val toolbarTitle = "New Title"
    testRegisterActivity.switchFragment(
      tag = TestFragment.TAG + 2,
      isRegisterFragment = false,
      isFilterVisible = false,
      toolbarTitle = toolbarTitle
    )
    val registerActivityBinding = testRegisterActivity.registerActivityBinding

    Assert.assertFalse(
      registerActivityBinding.toolbarLayout.registerFilterTextview.hasOnClickListeners()
    )

    Assert.assertEquals(
      toolbarTitle,
      registerActivityBinding.toolbarLayout.registerFilterTextview.text.toString()
    )
  }

  @Test
  @Ignore("Figure out how to set permission")
  fun testBarcodeScanButton_onClick_with_permission() {
    val testRegisterActivitySpy = spyk(testRegisterActivity)
    every {
      testRegisterActivitySpy.checkPermission(Manifest.permission.CAMERA, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
    testRegisterActivitySpy.registerActivityBinding.toolbarLayout.btnScanBarcode.performClick()
    Assert.assertTrue(testRegisterActivitySpy.liveBarcodeScanningFragment.isVisible)
    testRegisterActivitySpy.finish()
  }

  @Test
  fun testHandleSyncFailed_should_verifyAllInternalState() {

    every { accountAuthenticator.logout() } returns Unit

    val glitchState =
      State.Glitch(
        listOf(
          mockk {
            every { exception } returns mockk<HttpException> { every { code() } returns 401 }
          }
        )
      )

    handleSyncFailed(glitchState)
    verify(exactly = 1) { accountAuthenticator.logout() }

    val failedState =
      State.Failed(
        Result.Error(
          listOf(
            mockk {
              every { exception } returns mockk<HttpException> { every { code() } returns 401 }
            }
          )
        )
      )

    handleSyncFailed(failedState)
    verify(exactly = 1, inverse = true) { accountAuthenticator.logout() }

    val glitchStateInterruptedIOException =
      State.Glitch(
        listOf(
          mockk {
            every { exception } returns
              mockk<InterruptedIOException> {
                every { message } returns "java.io.InterruptedIOException: timeout"
              }
          }
        )
      )

    handleSyncFailed(glitchStateInterruptedIOException)
    Assert.assertEquals(
      View.GONE,
      testRegisterActivity.registerActivityBinding.progressSync.visibility
    )
    Assert.assertNotNull(
      testRegisterActivity.registerActivityBinding.containerProgressSync.background
    )

    val glitchStateUnknownHostException =
      State.Glitch(
        listOf(
          mockk {
            every { exception } returns
              mockk<UnknownHostException> {
                every { message } returns
                  "java.net.UnknownHostException: Unable to resolve host fhir.labs.smartregister.org: No address associated with hostname"
              }
          }
        )
      )

    handleSyncFailed(glitchStateUnknownHostException)
    Assert.assertEquals(
      View.GONE,
      testRegisterActivity.registerActivityBinding.progressSync.visibility
    )
    Assert.assertNotNull(
      testRegisterActivity.registerActivityBinding.containerProgressSync.background
    )

    handleSyncFailed(State.Glitch(listOf()))
    Assert.assertFalse(
      testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
    )
    Assert.assertEquals(
      View.GONE,
      testRegisterActivity.registerActivityBinding.progressSync.visibility
    )
    Assert.assertNotNull(
      testRegisterActivity.registerActivityBinding.containerProgressSync.background
    )
    Assert.assertTrue(
      testRegisterActivity.registerActivityBinding.containerProgressSync.hasOnClickListeners()
    )
  }

  private fun handleSyncFailed(state: State) {
    ReflectionHelpers.callInstanceMethod<Any>(
      testRegisterActivity,
      "handleSyncFailed",
      ReflectionHelpers.ClassParameter(State::class.java, state)
    )
  }

  @AndroidEntryPoint
  class TestRegisterActivity : BaseRegisterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val registerViewConfiguration = registerViewConfigurationOf("appId")
      configureViews(registerViewConfiguration)
    }

    override fun mainFragmentTag() = TestFragment.TAG + 1

    override fun sideMenuOptions() =
      listOf(
        SideMenuOption(
          itemId = R.id.menu_item_clients,
          titleResource = R.string.clients,
          iconResource = ContextCompat.getDrawable(this, R.drawable.ic_menu)!!,
          count = 10,
          countMethod = { 10L }
        )
      )

    override fun supportedFragments() =
      mapOf(
        Pair(TestFragment.TAG + 1, TestFragment(1)),
        Pair(TestFragment.TAG + 2, TestFragment(2))
      )

    override fun registersList() =
      listOf(RegisterItem(TestFragment.TAG + 1, "TestFragment", isSelected = true))
  }

  @AndroidEntryPoint
  class TestFragment(private val number: Int) : Fragment() {
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
      val composeView = ComposeView(requireContext())
      composeView.setContent { Text(text = "Hello Fragment: $number") }
      return composeView
    }

    companion object {
      const val TAG = "TestFragment"
    }
  }

  enum class TestConfigClassification : ConfigClassification {
    PATIENT_REGISTER;

    override val classification: String = name.lowercase()
  }
}
