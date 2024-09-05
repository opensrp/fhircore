/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import javax.inject.Inject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.ResourceType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.LocationLogOptions
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class QuestionnaireActivityTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine
  private val context: Application = ApplicationProvider.getApplicationContext()
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var questionnaireJson: String
  private lateinit var questionnaire: Questionnaire
  private lateinit var questionnaireActivityController: ActivityController<QuestionnaireActivity>
  private lateinit var questionnaireActivity: QuestionnaireActivity

  @Inject lateinit var testDispatcherProvider: DispatcherProvider

  @BindValue lateinit var defaultRepository: DefaultRepository

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply {
      setTheme(org.smartregister.fhircore.engine.R.style.AppTheme)
    }
    defaultRepository =
      mockk(relaxUnitFun = true) {
        every { dispatcherProvider } returns testDispatcherProvider
        every { fhirEngine } returns spyk(this@QuestionnaireActivityTest.fhirEngine)
      }
    questionnaireConfig =
      QuestionnaireConfig(
        id = "754", // Same as ID in sample_patient_registration.json
        title = "Patient registration",
        type = "DEFAULT",
        extraParams =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "household.id",
              dataType = Enumerations.DataType.INTEGER,
              key = "opensrpId",
              value = "@{humanReadableId}",
            ),
          ),
        configRules =
          listOf(
            RuleConfig(
              name = "humanReadableId",
              description = "Generate OpenSRP ID",
              condition = "true",
              actions =
                listOf(
                  "data.put('humanReadableId', service.generateRandomSixDigitInt())",
                ),
            ),
          ),
      )
    questionnaireJson =
      context.assets.open("resources/sample_patient_registration.json").bufferedReader().use {
        it.readText()
      }
    questionnaire = questionnaireJson.decodeResourceFromString()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    if (this::questionnaireActivityController.isInitialized) {
      questionnaireActivityController.destroy()
    }
  }

  @Test
  fun testThatActivityIsFinishedIfQuestionnaireConfigIsMissing() = runTest {
    questionnaireActivityController = Robolectric.buildActivity(QuestionnaireActivity::class.java)
    questionnaireActivity = questionnaireActivityController.create().resume().get()
    Assert.assertEquals(
      "QuestionnaireConfig is required but missing.",
      ShadowToast.getTextOfLatestToast(),
    )
  }

  @Test
  fun testThatActivityFinishesWhenQuestionnaireIsNull() = runTest {
    coEvery { defaultRepository.fhirEngine.get(any<ResourceType>(), any<String>()) } answers
      {
        throw ResourceNotFoundException(firstArg<ResourceType>().name, secondArg())
      }
    mockkStatic(Toast::class)
    every { Toast.makeText(any(), any<String>(), Toast.LENGTH_LONG) } returns
      mockk<Toast>() { every { show() } just runs }
    setupActivity()
    advanceUntilIdle()
    verify { Toast.makeText(any(), eq(context.getString(R.string.questionnaire_not_found)), any()) }
    unmockkStatic(Toast::class)
  }

  @Test
  fun testThatActivityRendersConfiguredQuestionnaire() =
    runTest(timeout = 90.seconds) {
      // TODO verify that this test executes as expected

      // Questionnaire will be retrieved from the database
      fhirEngine.create(questionnaire.apply { id = questionnaireConfig.id })

      setupActivity()
      Assert.assertTrue(questionnaireActivity.supportFragmentManager.fragments.isNotEmpty())
      val firstFragment = questionnaireActivity.supportFragmentManager.fragments.firstOrNull()
      Assert.assertTrue(firstFragment is QuestionnaireFragment)

      // Questionnaire should be the same
      val fragmentQuestionnaire =
        questionnaireActivity.supportFragmentManager.fragments
          .firstOrNull()
          ?.arguments
          ?.getString("questionnaire")
          ?.decodeResourceFromString<Questionnaire>()

      Assert.assertEquals(questionnaire.id, fragmentQuestionnaire?.id)
      val sortedQuestionnaireItemLinkIds =
        questionnaire.item.map { it.linkId }.sorted().joinToString(",")
      val sortedFragmentQuestionnaireItemLinkIds =
        fragmentQuestionnaire?.item?.map { it.linkId }?.sorted()?.joinToString(",")

      Assert.assertEquals(sortedQuestionnaireItemLinkIds, sortedFragmentQuestionnaireItemLinkIds)
    }

  @Test
  fun testThatOnBackPressShowsConfirmationAlertDialog() =
    runTest(UnconfinedTestDispatcher()) {
      setupActivity()
      questionnaireActivity.onBackPressedDispatcher.onBackPressed()
      val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
      Assert.assertNotNull(dialog)
    }

  @Test
  fun `setupLocationServices should open location settings if location is disabled`() {
    setupActivity()
    assertTrue(
      questionnaireActivity.viewModel.applicationConfiguration.logGpsLocation.contains(
        LocationLogOptions.QUESTIONNAIRE,
      ),
    )

    val fusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(questionnaireActivity)
    assertNotNull(fusedLocationProviderClient)

    shadowOf(questionnaireActivity)
      .grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val locationManager =
      questionnaireActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
    locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

    questionnaireActivity.fetchLocation()
    val startedIntent = shadowOf(questionnaireActivity).nextStartedActivity
    val expectedIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    assertEquals(expectedIntent.component, startedIntent.component)
  }

  private fun setupActivity() {
    val bundle = QuestionnaireActivity.intentBundle(questionnaireConfig, emptyList())
    questionnaireActivityController =
      Robolectric.buildActivity(
        QuestionnaireActivity::class.java,
        Intent().apply { putExtras(bundle) },
      )
    questionnaireActivity = questionnaireActivityController.create().resume().get()
  }
}
