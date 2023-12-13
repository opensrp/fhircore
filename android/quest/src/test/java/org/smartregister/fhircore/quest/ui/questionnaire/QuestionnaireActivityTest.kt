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

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.setFragmentResult
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Date
import javax.inject.Inject
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfirmationDialog
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_BARCODE
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG

@HiltAndroidTest
class QuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator
  @Inject lateinit var jsonParser: IParser
  @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireFragment: QuestionnaireFragment
  private lateinit var intent: Intent
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())
  private val fhirEngine: FhirEngine = mockk()
  private val defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine, dispatcherProvider, mockk(), mockk(), mockk(), mockk(), mockk())

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(
      QuestionnaireViewModel(
        defaultRepository = defaultRepository,
        configurationRegistry = mockk(),
        transformSupportServices = mockk(),
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = mockk(),
        libraryEvaluator = mockk(),
        fhirCarePlanGenerator = mockk(),
        resourceDataRulesExecutor = mockk()
      )
    )

  @Before
  fun setUp() {
    // TODO Proper set up
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    val questionnaireConfigRules =
      listOf(
        RuleConfig(
          name = "humanReadableId",
          description = "Generate OpenSRP ID",
          condition = "true",
          actions = listOf("data.put('humanReadableId', service.generateRandomSixDigitInt())")
        )
      )
    questionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        "form",
        resourceIdentifier = "Patient/P1",
        extraParams =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "household.id",
              dataType = DataType.INTEGER,
              key = "opensrpId",
              value = "@{humanReadableId}"
            )
          ),
        configRules = questionnaireConfigRules
      )
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "25cc8d26-ac42-475f-be79-6f1d62a44881",
          dataType = DataType.INTEGER,
          key = "maleCondomPreviousBalance",
          value = "100"
        )
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, questionnaireConfig),
            Pair(QuestionnaireActivity.QUESTIONNAIRE_ACTION_PARAMETERS, actionParams)
          )
        )

    coEvery {
      questionnaireViewModel.computeQuestionnaireConfigRules(questionnaireConfigRules)
    } returns mapOf("humanReadableId" to "199290")
    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply { id = "12345" }

    buildActivity(buildQuestionnaireWithConstraints(), questionnaireConfig)
  }

  private fun buildActivity(
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig
  ) {
    questionnaireFragment = spyk()

    questionnaireFragment.apply {
      arguments = bundleOf(Pair("questionnaire", questionnaire.encodeResourceToString()))
    }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }
    ReflectionHelpers.setField(questionnaireActivity, "questionnaireConfig", questionnaireConfig)
  }

  @After
  fun cleanup() {
    unmockkObject(SharedPreferencesHelper)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testIntentArgsShouldInsertValues() {
    val questionnaireResponse = QuestionnaireResponse()
    val patient = Patient().apply { id = "my-patient-id" }
    val populationResources = ArrayList<Resource>()
    populationResources.add(patient)
    val result =
      QuestionnaireActivity.intentArgs(
        questionnaireResponse = questionnaireResponse,
        populationResources = populationResources,
        questionnaireConfig =
          QuestionnaireConfig(
            id = "my-form",
            resourceIdentifier = "1234",
            type = QuestionnaireType.READ_ONLY
          ),
        actionParams =
          listOf(
            ActionParameter(
              paramType = ActionParameterType.PREPOPULATE,
              linkId = "my-param",
              dataType = DataType.INTEGER,
              key = "my-key",
              value = "100"
            )
          )
      )

    val actualQuestionnaireConfig =
      result.getSerializable(QuestionnaireActivity.QUESTIONNAIRE_CONFIG) as QuestionnaireConfig
    val actualActionParams =
      result.getSerializable(QuestionnaireActivity.QUESTIONNAIRE_ACTION_PARAMETERS) as
        List<ActionParameter>
    Assert.assertEquals("my-form", actualQuestionnaireConfig.id)
    Assert.assertEquals("1234", actualQuestionnaireConfig.resourceIdentifier)
    Assert.assertEquals(1, actualActionParams.size)
    Assert.assertEquals("my-param", actualActionParams[0].linkId)
    Assert.assertEquals(QuestionnaireType.READ_ONLY.name, actualQuestionnaireConfig.type.name)
    Assert.assertEquals(
      FhirContext.forCached(FhirVersionEnum.R4)
        .newJsonParser()
        .encodeResourceToString(questionnaireResponse),
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE)
    )
    Assert.assertEquals(
      FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().encodeResourceToString(patient),
      result.getStringArrayList(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES)?.get(0)
    )
    Assert.assertEquals(1, actualActionParams.size)
    Assert.assertEquals("my-param", actualActionParams[0].linkId)
  }

  @Test
  fun testReadOnlyIntentShouldBeReadToReadOnlyFlag() {
    Assert.assertFalse(questionnaireConfig.type.isReadOnly())
    val expectedQuestionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.READ_ONLY
      )
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param",
          dataType = DataType.INTEGER,
          key = "my-key",
          value = "100"
        )
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, expectedQuestionnaireConfig),
            Pair(QuestionnaireActivity.QUESTIONNAIRE_ACTION_PARAMETERS, actionParams)
          )
        )

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val updatedQuestionnaireConfig =
      ReflectionHelpers.getField<QuestionnaireConfig>(questionnaireActivity, "questionnaireConfig")
    Assert.assertTrue(updatedQuestionnaireConfig.type.isReadOnly())
  }

  @Test
  fun testPrePopulationParamsFiltersEmptyAndNonInterpolatedValues() {
    Assert.assertFalse(questionnaireConfig.type.isReadOnly())
    val expectedQuestionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.READ_ONLY
      )
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param1",
          dataType = DataType.INTEGER,
          key = "my-key",
          value = "100"
        ),
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param2",
          dataType = DataType.STRING,
          key = "my-key",
          value = "@{value}"
        ),
        ActionParameter(
          paramType = ActionParameterType.PREPOPULATE,
          linkId = "my-param2",
          dataType = DataType.STRING,
          key = "my-key",
          value = ""
        ),
        ActionParameter(key = "patientId", value = "patient-id")
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, expectedQuestionnaireConfig),
            Pair(QuestionnaireActivity.QUESTIONNAIRE_ACTION_PARAMETERS, actionParams)
          )
        )

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val updatedActionParams =
      ReflectionHelpers.getField<List<ActionParameter>>(questionnaireActivity, "actionParams")
    val prePopulationParams =
      ReflectionHelpers.getField<List<ActionParameter>>(
        questionnaireActivity,
        "prePopulationParams"
      )
    Assert.assertEquals(4, updatedActionParams.size)
    Assert.assertEquals(1, prePopulationParams.size)
    Assert.assertEquals("my-param1", prePopulationParams[0].linkId)
  }

  @Test
  fun testGetQuestionnaireResponseShouldHaveSubjectAndDate() {
    val questionnaire = Questionnaire().apply { id = "12345" }
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    var questionnaireResponse = QuestionnaireResponse()

    Assert.assertNull(questionnaireResponse.id)
    Assert.assertNull(questionnaireResponse.authored)

    questionnaireResponse = questionnaireActivity.getQuestionnaireResponse()

    Assert.assertNotNull(questionnaireResponse.id)
    Assert.assertNotNull(questionnaireResponse.authored)
    Assert.assertEquals(
      "Patient/${questionnaireConfig.resourceIdentifier}",
      questionnaireResponse.subject.reference
    )
    Assert.assertEquals("Questionnaire/12345", questionnaireResponse.questionnaire)
  }

  @Test
  fun testOnBackPressedShouldShowConfirmAlert() {
    questionnaireActivity.onBackPressed()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_button_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  @Test
  fun testOnBackPressedWithSaveDraftEnabledShouldShowCancelAlert() {
    questionnaireConfig = questionnaireConfig.copy(saveDraft = true)
    ReflectionHelpers.setField(questionnaireActivity, "questionnaireConfig", questionnaireConfig)
    questionnaireActivity.onBackPressed()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_in_progress_alert_back_pressed_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_save_draft_button_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  @Test
  fun testHandleSaveDraftQuestionnaireShowsProgressAlertAndCallsHandlePartialResponse() {
    questionnaireConfig = questionnaireConfig.copy(saveDraft = true)
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    ReflectionHelpers.setField(questionnaireActivity, "questionnaireConfig", questionnaireConfig)

    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireViewModel.partialQuestionnaireResponseHasValues(any()) } returns true
    questionnaireActivity.handleSaveDraftQuestionnaire()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.form_progress_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(timeout = 2000) { questionnaireActivity.handlePartialQuestionnaireResponse(any()) }
  }

  @Test
  fun testHandlePartialQuestionnaireResponseCallsSavePartialResponse() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    questionnaireActivity.handlePartialQuestionnaireResponse(QuestionnaireResponse())
    verify { questionnaireViewModel.savePartialQuestionnaireResponse(any(), any()) }
  }

  @Test
  fun testOnBackPressedShouldCallFinishWhenInReadOnlyMode() {
    val qActivity = spyk(questionnaireActivity)
    questionnaireConfig = questionnaireConfig.copy(type = QuestionnaireType.READ_ONLY)
    ReflectionHelpers.setField(qActivity, "questionnaireConfig", questionnaireConfig)
    qActivity.onBackPressed()

    verify { qActivity.finish() }
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())

    questionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    verify {
      questionnaireViewModel.extractAndSaveResources(
        any(),
        any(),
        any(),
        any(),
      )
    }
  }

  // TODO: https://github.com/opensrp/fhircore/issues/2494 - fix this test.
  @Ignore
  @Test
  fun testHandleQuestionnaireResponseThroughADialogShouldCallExtractAndSaveResources() {
    val questionnaireResponseId = "patient-registration-response"
    val questionnaireResponse = QuestionnaireResponse().apply { id = questionnaireResponseId }
    questionnaireConfig =
      QuestionnaireConfig(
        id = questionnaireResponseId,
        title = "Patient registration",
        "form",
        confirmationDialog =
          ConfirmationDialog(
            title = "title",
            message = "message",
            actionButtonText = "Save changes",
          )
      )

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    ReflectionHelpers.setField(questionnaireActivity, "questionnaireConfig", questionnaireConfig)

    questionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")
    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

    coEvery { fhirEngine.get(any(), any()) } returns questionnaireResponse
    coEvery { fhirEngine.update(any()) } returns Unit

    verify { questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any()) }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowProgressAndCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    ReflectionHelpers.setField(questionnaireActivity, "questionnaireConfig", questionnaireConfig)

    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.form_progress_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(timeout = 2000) {
      questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any())
    }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowErrorAlertOnInvalidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    coEvery { questionnaireViewModel.defaultRepository.addOrUpdate(resource = any()) } just runs
    every { questionnaireFragment.getQuestionnaireResponse() } returns
      QuestionnaireResponse().apply {
        addItem().apply { linkId = "1" }
        addItem().apply { linkId = "2" }
      }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_invalid_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(inverse = true) {
      questionnaireViewModel.extractAndSaveResources(any(), any(), any(), any())
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun testOnClickEditButtonShouldSetEditModeToTrue() = runTest {
    val questionnaire = Questionnaire().apply { experimental = false }
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    Assert.assertFalse(questionnaireConfig.type.isEditMode())

    questionnaireActivity.onClick(questionnaireActivity.findViewById(R.id.btn_edit_qr))

    val updatedQuestionnaireConfig =
      ReflectionHelpers.getField<QuestionnaireConfig>(questionnaireActivity, "questionnaireConfig")
    Assert.assertTrue(updatedQuestionnaireConfig.type.isEditMode())
  }

  @Test
  fun testValidQuestionnaireResponseShouldReturnTrueForValidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.apply {
      addItem().apply {
        addAnswer().apply { this.value = StringType("v1") }
        linkId = "1"
      }
      addItem().apply {
        addAnswer().apply { this.value = StringType("") }
        linkId = "3"
      }
    }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    val result = questionnaireActivity.validQuestionnaireResponse(questionnaireResponse)

    Assert.assertTrue(result)
  }

  @Test
  fun testValidQuestionnaireResponseShouldReturnFalseForInvalidData() {
    val questionnaire = buildQuestionnaireWithConstraints()

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.apply { addItem().apply { linkId = "1" } }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    val result = questionnaireActivity.validQuestionnaireResponse(questionnaireResponse)

    Assert.assertFalse(result)
  }

  @Test
  fun testValidQuestionnaireResponseShouldReturnFalseForInvalidDataWhenHiddenIsFalse() {
    val questionnaire = buildQuestionnaireWithConstraints()
    questionnaire.apply {
      addItem().apply {
        required = true
        linkId = "4"
        type = Questionnaire.QuestionnaireItemType.STRING
        addExtension(
          Extension(
            "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
            BooleanType(false)
          )
        )
      }
    }

    val questionnaireResponse = QuestionnaireResponse()
    questionnaireResponse.apply {
      addItem().apply {
        addAnswer().apply { this.value = StringType("v1") }
        linkId = "1"
      }
      addItem().apply {
        addAnswer().apply { this.value = StringType("") }
        linkId = "4"
      }
    }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    val result = questionnaireActivity.validQuestionnaireResponse(questionnaireResponse)

    Assert.assertFalse(result)
  }

  @Test
  fun testPostSaveSuccessfulShouldFinishActivity() {
    val questionnaire = buildQuestionnaireWithConstraints()
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    questionnaireActivity.postSaveSuccessful(QuestionnaireResponse())

    Assert.assertTrue(questionnaireActivity.isFinishing)
  }

  @Test
  fun testPostSaveSuccessfulWithExtractionMessageShouldShowAlert() {
    questionnaireActivity.questionnaireViewModel.extractionProgressMessage.postValue("ABC")
    questionnaireActivity.postSaveSuccessful(QuestionnaireResponse())

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals("ABC", alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text)
  }

  @Test
  fun onOptionsItemSelectedShouldCallOnBackPressedWhenHomeIsPressed() {
    val spiedActivity = spyk(questionnaireActivity)
    val menuItem = mockk<MenuItem>()
    every { menuItem.itemId } returns android.R.id.home
    every { spiedActivity.onBackPressed() } just runs

    Assert.assertTrue(spiedActivity.onOptionsItemSelected(menuItem))

    verify { spiedActivity.onBackPressed() }
  }

  @Test
  fun onPostSaveShouldCallPostSaveSuccessfulWhenResultIsTrue() {
    val spiedActivity = spyk(questionnaireActivity)
    val qr = mockk<QuestionnaireResponse>()
    every { qr.copy() } returns qr
    every { spiedActivity.postSaveSuccessful(qr) } just runs

    spiedActivity.onPostSave(true, qr)

    verify { spiedActivity.postSaveSuccessful(qr) }
    verify { spiedActivity.dismissSaveProcessing() }
  }

  @Test
  fun testQuestionnaireTypeEditShouldAppendEditPrefixInActionBarTitle() {
    with(questionnaireActivity) {
      val questionnaireConfig =
        QuestionnaireConfig("form", "title", "form-id", type = QuestionnaireType.EDIT)
      ReflectionHelpers.setField(this, "questionnaireConfig", questionnaireConfig)

      updateViews()

      Assert.assertEquals("${getString(R.string.edit)} title", supportActionBar?.title)
    }
  }

  @Test
  fun testQuestionnaireTypeDefaultShouldHasNormalActionBarTitle() {
    with(questionnaireActivity) {
      questionnaireConfig.copy(type = QuestionnaireType.DEFAULT)
      val questionnaireConfig = QuestionnaireConfig("form", "title", "form-id")
      ReflectionHelpers.setField(this, "questionnaireConfig", questionnaireConfig)

      updateViews()

      Assert.assertEquals("title", supportActionBar?.title)
    }
  }

  @Test
  fun testQuestionnaireTypeReadOnlyShouldHasNormalActionBarTitle() {
    with(questionnaireActivity) {
      questionnaireConfig.copy(type = QuestionnaireType.READ_ONLY)
      val questionnaireConfig = QuestionnaireConfig("form", "title", "form-id")
      ReflectionHelpers.setField(this, "questionnaireConfig", questionnaireConfig)

      updateViews()

      Assert.assertEquals("title", questionnaireActivity.supportActionBar?.title)
    }
  }
  @Test
  fun testQuestionnaireSubmitButtonDisplayCorrectTitle() {
    with(questionnaireActivity) {
      questionnaireConfig.copy(type = QuestionnaireType.READ_ONLY)
      val questionnaireConfig =
        QuestionnaireConfig("form", "title", "form-id", type = QuestionnaireType.READ_ONLY)
      ReflectionHelpers.setField(this, "questionnaireConfig", questionnaireConfig)

      updateViews()
      val button = findViewById<Button>(org.smartregister.fhircore.quest.R.id.submit_questionnaire)
      if (button != null) {
        Assert.assertEquals("${getString(R.string.done)}", button.text.toString())
      }
    }
  }

  private fun buildQuestionnaireWithConstraints(): Questionnaire {
    return Questionnaire().apply {
      addItem().apply {
        required = true
        linkId = "1"
        type = Questionnaire.QuestionnaireItemType.STRING
      }
      addItem().apply {
        maxLength = 4
        linkId = "2"
        type = Questionnaire.QuestionnaireItemType.STRING
      }
      addItem().apply {
        required = true
        linkId = "3"
        type = Questionnaire.QuestionnaireItemType.STRING
        addExtension(
          Extension(
            "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
            BooleanType(true)
          )
        )
      }
    }
  }

  @Ignore("Needs fixing")
  @Test
  fun testFragmentQuestionnaireSubmitHandlesQuestionnaireSubmit() {

    every { questionnaireActivity.getQuestionnaireConfig() } returns QuestionnaireConfig(id = "123")
    every { questionnaireActivity.getQuestionnaireObject() } returns Questionnaire()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireActivity.finish() } just runs
    every { questionnaireActivity.handleQuestionnaireSubmit() } just runs

    questionnaireActivity = spyk(questionnaireActivity)
    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireActivity.handleQuestionnaireSubmit() }
  }

  @Ignore("Needs fixing")
  @Test
  fun testFragmentQuestionnaireSubmitWithReadOnlyModeFinishesActivity() {
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireActivity.finish() } just runs
    every { questionnaireActivity.getQuestionnaireConfig().type.isReadOnly() } returns true

    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireActivity.finish() }
  }
  @Ignore("Needs fixing")
  @Test
  fun testFragmentQuestionnaireSubmitWithExperimentalModeFinishesActivity() {
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireActivity.finish() } just runs
    every { questionnaireActivity.getQuestionnaireObject().experimental } returns true
    every { questionnaireActivity.getQuestionnaireConfig() } returns QuestionnaireConfig(id = "123")

    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireActivity.finish() }
  }

  @Test
  fun testRemoveOperationWithValueOfTrueFinishesActivity() {
    questionnaireActivity.questionnaireViewModel.removeOperation.postValue(true)
    assertTrue { questionnaireActivity.isFinishing }
  }

  @Test
  fun testQuestionnaireShouldExist() {
    val expectedQuestionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.DEFAULT
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, expectedQuestionnaireConfig))
        )

    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply { id = "12345" }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val questionnaire =
      ReflectionHelpers.getField<Questionnaire>(questionnaireActivity, "questionnaire")
    Assert.assertNotNull(questionnaire)
  }

  @Test
  fun testQuestionnaireShouldNotExist() {
    val expectedQuestionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.DEFAULT
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, expectedQuestionnaireConfig))
        )

    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns null

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val questionnaire =
      ReflectionHelpers.getField<Questionnaire>(questionnaireActivity, "questionnaire")
    Assert.assertNull(questionnaire)
  }

  @Test
  fun testQuestionnaireShouldSetBarcode() {
    val expectedQuestionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        resourceIdentifier = "1234",
        type = QuestionnaireType.DEFAULT
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, expectedQuestionnaireConfig))
        )

    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply {
        id = "12345"
        addItem().apply { linkId = QUESTIONNAIRE_ARG_BARCODE }
      }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val questionnaire =
      ReflectionHelpers.getField<Questionnaire>(questionnaireActivity, "questionnaire")
    Assert.assertNotNull(questionnaire.find(QUESTIONNAIRE_ARG_BARCODE))
    Assert.assertTrue(
      "Barcode initial not found with ID 1234",
      questionnaire.find(QUESTIONNAIRE_ARG_BARCODE)!!.initial.first().valueStringType.value ==
        "1234"
    )
    Assert.assertTrue(
      "Barcode not in read only mode",
      questionnaire.find(QUESTIONNAIRE_ARG_BARCODE)!!.readOnly
    )
  }

  @Test
  fun testQuestionnaireResponseShouldExistInEditMode() {
    val questionnaireId = "patient-registration"
    val questionnaireConfig =
      QuestionnaireConfig(
        id = questionnaireId,
        title = "Patient registration",
        type = QuestionnaireType.EDIT
      )
    val baseResourceId = "Patient/1122"
    val baseResourceType = "Patient"
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, questionnaireConfig),
            Pair(QuestionnaireActivity.BASE_RESOURCE_ID, baseResourceId),
            Pair(QuestionnaireActivity.BASE_RESOURCE_TYPE, baseResourceType)
          )
        )

    val questionnaireResponseId = "patient-registration-response"
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply { id = questionnaireId }
    coEvery {
      questionnaireViewModel.defaultRepository.fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = baseResourceId })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/$questionnaireId" })
      }
    } returns
      listOf(
        QuestionnaireResponse().apply {
          id = questionnaireResponseId
          meta.lastUpdated = Date()
        }
      )

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val questionnaireResponse =
      ReflectionHelpers.getField<QuestionnaireResponse>(
        questionnaireActivity,
        "questionnaireResponse"
      )
    Assert.assertNotNull("Questionnaire Response is null", questionnaireResponse)
  }

  @Test
  fun testQuestionnaireResponseShouldExistInReadOnlyMode() {
    val questionnaireId = "patient-registration"
    val questionnaireConfig =
      QuestionnaireConfig(
        id = questionnaireId,
        title = "Patient registration",
        type = QuestionnaireType.READ_ONLY
      )
    val baseResourceId = "Patient/1122"
    val baseResourceType = "Patient"
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, questionnaireConfig),
            Pair(QuestionnaireActivity.BASE_RESOURCE_ID, baseResourceId),
            Pair(QuestionnaireActivity.BASE_RESOURCE_TYPE, baseResourceType)
          )
        )

    val questionnaireResponseId = "patient-registration-response"
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply { id = questionnaireId }
    coEvery {
      questionnaireViewModel.defaultRepository.fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = baseResourceId })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/$questionnaireId" })
      }
    } returns
      listOf(
        QuestionnaireResponse().apply {
          id = questionnaireResponseId
          meta.lastUpdated = Date()
        }
      )

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val questionnaireResponse =
      ReflectionHelpers.getField<QuestionnaireResponse>(
        questionnaireActivity,
        "questionnaireResponse"
      )
    Assert.assertNotNull("Questionnaire Response is null", questionnaireResponse)
  }

  @Test
  fun `test covid 19 vaccines questionnaire on followup`() = runTest {
    ReflectionHelpers.loadClass(
        QuestionnaireFragment.javaClass.classLoader,
        "com.google.android.fhir.datacapture.DataCapture"
      )
      .let {
        it.getDeclaredField("configuration")
          .also { it.isAccessible = true }
          .set(null, DataCaptureConfig(xFhirQueryResolver = { fhirEngine.search(it) }))
      }
    val questionnaire =
      "covid-19/questionnaire.json".readFile().decodeResourceFromString<Questionnaire>()
    val data =
      "covid-19/resource_data_bundle.json"
        .readFile()
        .decodeResourceFromString<org.hl7.fhir.r4.model.Bundle>()

    coEvery { fhirEngine.get(ResourceType.Patient, "P1") } returns
      Patient().apply {
        id = "P1"
        birthDate = Date()
      }
    coEvery { fhirEngine.search<Resource>(any<Search>()) } returns
      data.entry.map { it.resource as Immunization }
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns questionnaire

    buildActivity(questionnaire, questionnaireConfig)

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    questionnaireActivity.fragment = questionnaireFragment

    val questionnaireResponse = questionnaireActivity.getQuestionnaireResponse()
    questionnaireActivity.setInitialExpression(questionnaire)

    Assert.assertNotNull(questionnaireResponse.id)
    Assert.assertNotNull(questionnaireResponse.authored)
    Assert.assertEquals(
      "Patient/${questionnaireConfig.resourceIdentifier}",
      questionnaireResponse.subject.reference
    )
    Assert.assertEquals(2, questionnaire.item.elementAt(1).initial.size)
  }

  fun testGetResourcesFromParamsForQR_shouldFilterQuestionnaireResponsePopulationParam() {
    val questionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        type = QuestionnaireType.EDIT
      )
    val actionParams =
      listOf(
        ActionParameter(
          paramType = ActionParameterType.QUESTIONNAIRE_RESPONSE_POPULATION_RESOURCE,
          resourceType = ResourceType.Patient,
          key = "resourcePatient",
          value = "patient-1",
          dataType = DataType.STRING
        ),
        ActionParameter(
          key = "paramName",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing",
          dataType = DataType.STRING,
          linkId = null
        ),
        ActionParameter(
          key = "paramName2",
          paramType = ActionParameterType.PREPOPULATE,
          value = "testing2",
          dataType = DataType.STRING,
          linkId = null
        )
      )
    intent =
      Intent()
        .putExtras(
          bundleOf(
            Pair(QuestionnaireActivity.QUESTIONNAIRE_CONFIG, questionnaireConfig),
            Pair(QuestionnaireActivity.QUESTIONNAIRE_ACTION_PARAMETERS, actionParams)
          )
        )

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    val resourceMap = questionnaireActivity.getResourcesFromParamsForQR()

    Assert.assertEquals(1, resourceMap.size)
    Assert.assertTrue(resourceMap.containsKey(ResourceType.Patient))
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
