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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.spyk
import java.time.OffsetDateTime
import javax.inject.Inject
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
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigClassification
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.extension.asString

@HiltAndroidTest
class BaseRegisterActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var testRegisterActivityController: ActivityController<TestRegisterActivity>

  private lateinit var testRegisterActivity: TestRegisterActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    configurationRegistry.loadAppConfigurations(
      appId = "appId",
      accountAuthenticator = accountAuthenticator
    ) {}
    testRegisterActivityController = Robolectric.buildActivity(TestRegisterActivity::class.java)
    testRegisterActivity = testRegisterActivityController.get()
    testRegisterActivityController.create().resume()
  }

  override fun tearDown() {
    // Reset syncBroadcaster
    testRegisterActivity.syncBroadcaster.syncInitiator = null
    testRegisterActivity.syncBroadcaster.syncListeners.clear()
    super.tearDown()
  }
  override fun getActivity(): Activity = testRegisterActivity

  @Test
  fun testViewSetup() {

    Assert.assertTrue(testRegisterActivity.syncBroadcaster.syncInitiator is TestRegisterActivity)

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

    // Bottom navigation contains one menu option
    Assert.assertTrue(testRegisterActivity.bottomNavigationMenuOptions().isNotEmpty())
  }

  @Test
  fun testConfigureViewWithSideMenu() {
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
    val drawerLayout = registerActivityBinding.drawerLayout
    Assert.assertEquals(View.VISIBLE, drawerLayout.visibility)

    // New button visible, text also updated
    Assert.assertEquals(
      registerViewConfiguration.newClientButtonText,
      registerActivityBinding.btnRegisterNewClient.text
    )

    // Search bar is displayed
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

    // BottomNavigation Not visible
    Assert.assertEquals(View.GONE, registerActivityBinding.bottomNavView.visibility)
  }

  @Test
  fun testOnSyncWithSyncStatusStarted() {
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
  fun testOnSyncWithSyncStatusInProgress() {
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
  fun testOnSyncStatusFinished() {
    // Status Sync Finished
    val registerActivityBinding = testRegisterActivity.registerActivityBinding
    val result = spyk(Result.Success)
    val currentDateTime = OffsetDateTime.now()
    every { result.timestamp } returns currentDateTime
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
  fun testOnSyncStatusFailed() {
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
  fun testOnSyncStatusGlitch() {
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
  fun testOnNavigationSelectLanguageItemClickedShouldShowDialog() {
    val languageMenuItem = RoboMenuItem(R.id.menu_item_language)
    testRegisterActivity.onNavigationItemSelected(languageMenuItem)
    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    Assert.assertEquals(
      testRegisterActivity.getString(R.string.select_language),
      dialog.title,
    )
    Assert.assertEquals(R.drawable.ic_outline_language_black, dialog.iconId)
  }

  @Test
  fun testOnNavigationLogoutItemClickedShouldFinishActivity() {
    val logoutMenuItem = RoboMenuItem(R.id.menu_item_logout)
    testRegisterActivity.onNavigationItemSelected(logoutMenuItem)
    Assert.assertTrue(testRegisterActivity.isFinishing)
    Assert.assertFalse(
      testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
    )
  }

  @Test
  fun testOnNavigationItemClickedShouldCloseDrawer() {
    val clientsMenuItem = RoboMenuItem(R.id.menu_item_clients)
    testRegisterActivity.onNavigationItemSelected(clientsMenuItem)
    Assert.assertFalse(
      testRegisterActivity.registerActivityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)
    )
  }

  @Test
  fun testDestroyActivity() {
    testRegisterActivityController.pause().stop().destroy()
    // SyncListener removed from sync broadcaster and sync initiator set to null
    Assert.assertTrue(testRegisterActivity.syncBroadcaster.syncListeners.isEmpty())
    Assert.assertNull(testRegisterActivity.syncBroadcaster.syncInitiator)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testThatWrongDateThrowsAnException() {
    testRegisterActivity.registerViewModel.lastSyncTimestamp.value = "2021-12-15"
    Assert.assertTrue(
      testRegisterActivity.registerActivityBinding.tvLastSyncTimestamp.text.isEmpty()
    )
  }

  @Test
  fun testRegisterClientShouldLaunchQuestionnaireActivity() {
    testRegisterActivity.registerClient(null)
    val startedIntent: Intent = Shadows.shadowOf(testRegisterActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = Shadows.shadowOf(startedIntent)
    Assert.assertEquals(QuestionnaireActivity::class.java, shadowIntent.intentClass)
  }

  @Test
  fun testSwitchNonRegisterFragment() {
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
  fun testSwitchNonRegisterFragmentWithNewTitle() {
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
  fun testBarcodeScanButtonClickWithPermission() {
    val testRegisterActivitySpy = spyk(testRegisterActivity)
    every {
      testRegisterActivitySpy.checkPermission(Manifest.permission.CAMERA, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
    testRegisterActivitySpy.registerActivityBinding.toolbarLayout.btnScanBarcode.performClick()
    Assert.assertTrue(testRegisterActivitySpy.liveBarcodeScanningFragment.isVisible)
    testRegisterActivitySpy.finish()
  }

  @AndroidEntryPoint
  class TestRegisterActivity : BaseRegisterActivity() {

    @Inject lateinit var configurationRegistry: ConfigurationRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val registerViewConfiguration =
        configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
          configClassification = TestConfigClassification.PATIENT_REGISTER,
        )
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

    override fun bottomNavigationMenuOptions() =
      listOf(
        NavigationMenuOption(
          10000,
          getString(R.string.profile),
          ContextCompat.getDrawable(this, R.drawable.ic_user)!!
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
