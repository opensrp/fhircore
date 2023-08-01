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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowToast
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestionnaireActivityTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  private val context: Application = ApplicationProvider.getApplicationContext()
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var questionnaireJson: String
  private lateinit var questionnaire: Questionnaire
  private lateinit var questionnaireActivityController: ActivityController<QuestionnaireActivity>
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())
  private val fhirEngine: FhirEngine = mockk()
  private val defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine, dispatcherProvider, mockk(), mockk(), mockk(), mockk())

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(
      QuestionnaireViewModel(
        defaultRepository = defaultRepository,
        transformSupportServices = mockk(),
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = mockk(),
        libraryEvaluator = mockk(),
        fhirCarePlanGenerator = mockk(),
        resourceDataRulesExecutor = mockk(),
      ),
    )

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    questionnaireConfig =
      QuestionnaireConfig(
        id = "754", // Same as ID in sample_patient_registration.json
        title = "Patient registration",
        type = QuestionnaireType.DEFAULT,
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
              actions = listOf("data.put('humanReadableId', service.generateRandomSixDigitInt())"),
            ),
          ),
      )
    questionnaireJson =
      context.assets.open("sample_patient_registration.json").bufferedReader().use { it.readText() }
    questionnaire = questionnaireJson.decodeResourceFromString()

    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
  }

  @After
  override fun tearDown() {
    super.tearDown()
    questionnaireActivityController.destroy()
  }

  @Test
  fun testThatActivityIsFinishedIfQuestionnaireConfigIsMissing() {
    questionnaireActivityController = Robolectric.buildActivity(QuestionnaireActivity::class.java)
    questionnaireActivity = questionnaireActivityController.create().resume().get()
    Assert.assertEquals(
      "QuestionnaireConfig is required but missing.",
      ShadowToast.getTextOfLatestToast(),
    )
  }

  @Test
  fun testThatActivityRendersConfiguredQuestionnaire() = runTest {
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
  fun testThatOnBackPressShowsConfirmationAlertDialog() {
    setupActivity()
    questionnaireActivity.onBackPressedDispatcher.onBackPressed()
    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    Assert.assertNotNull(dialog)
  }

  private fun setupActivity() {
    coEvery {
      questionnaireViewModel.retrieveQuestionnaire(questionnaireConfig, emptyList())
    } returns questionnaire

    val bundle = QuestionnaireActivity.intentBundle(questionnaireConfig, emptyList())
    questionnaireActivityController =
      Robolectric.buildActivity(
        QuestionnaireActivity::class.java,
        Intent().apply { putExtras(bundle) },
      )
    questionnaireActivity = questionnaireActivityController.create().resume().get()
  }
}
