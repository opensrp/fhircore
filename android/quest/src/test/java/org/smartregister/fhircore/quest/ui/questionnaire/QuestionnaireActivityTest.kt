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
import com.google.android.fhir.datacapture.QuestionnaireFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.DataType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG

@HiltAndroidTest
class QuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var coroutinesTestRule = CoroutineTestRule()

  @Inject lateinit var fhirCarePlanGenerator: FhirCarePlanGenerator

  @Inject lateinit var jsonParser: IParser

  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireFragment: QuestQuestionnaireFragment

  private lateinit var intent: Intent

  val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())

  private lateinit var questionnaireConfig: QuestionnaireConfig

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(
      QuestionnaireViewModel(
        defaultRepository = mockk(),
        configurationRegistry = mockk(),
        transformSupportServices = mockk(),
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = mockk(),
        libraryEvaluator = mockk(),
        fhirCarePlanGenerator = mockk(),
        jsonParser = mockk()
      )
    )

  @Before
  fun setUp() {
    // TODO Proper set up
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    questionnaireConfig =
      QuestionnaireConfig(
        id = "patient-registration",
        title = "Patient registration",
        "form",
        resourceIdentifier = "@{familyLogicalId}",
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

    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply { id = "12345" }
    coEvery { questionnaireViewModel.generateQuestionnaireResponse(any(), any(), any()) } returns
      QuestionnaireResponse()

    questionnaireFragment = spyk()

    questionnaireFragment.apply {
      arguments =
        bundleOf(
          Pair(
            QuestionnaireFragment.EXTRA_QUESTIONNAIRE_JSON_STRING,
            buildQuestionnaireWithConstraints().encodeResourceToString()
          )
        )
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
  fun testOnClickEditButtonShouldSetEditModeToTrue() = runBlockingTest {
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

  @Test
  fun `Bundle#attachQuestionnaireResponse() should throw exception when QR not available and QuestionnaireConfig is readOnly`() {
    questionnaireConfig.type = QuestionnaireType.READ_ONLY

    Assertions.assertThrows(java.lang.IllegalArgumentException::class.java) {
      runBlocking {
        questionnaireActivity.attachQuestionnaireResponse(Bundle(), Intent(), questionnaireConfig)
      }
    }
  }

  @Test
  fun `Bundle#attachQuestionnaireResponse() should generate populated QR when population resources provided`() {
    val fhirJsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val patientString =
      fhirJsonParser.encodeResourceToString(Patient().apply { id = "my-patient-id" })
    val intent =
      Intent().apply { putExtra("questionnaire-population-resources", arrayListOf(patientString)) }

    val questionnaire =
      Questionnaire().apply {
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "patient-assigned-id"
            type = Questionnaire.QuestionnaireItemType.TEXT
            extension =
              listOf(
                Extension(
                  "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                  Expression().apply {
                    language = "text/fhirpath"
                    expression = "Patient.id"
                  }
                )
              )
          }
        )
      }

    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)

    runBlocking {
      questionnaireActivity.attachQuestionnaireResponse(Bundle(), intent, questionnaireConfig)
    }

    coVerify {
      questionnaireViewModel.generateQuestionnaireResponse(
        questionnaire,
        intent,
        questionnaireConfig
      )
    }
  }

  @Test
  fun `intentHasPopulationResources() should return false when questionnaire-population-resources extra is not set`() {
    assertFalse { questionnaireActivity.intentHasPopulationResources(Intent()) }
  }

  @Test
  fun `intentHasPopulationResources() should return true when questionnaire-population-resources extra is set`() {
    val patientString =
      FhirContext.forCached(FhirVersionEnum.R4)
        .newJsonParser()
        .encodeResourceToString(Patient().apply { id = "my-patient-id" })
    val intent =
      Intent().apply { putExtra("questionnaire-population-resources", arrayListOf(patientString)) }

    assertTrue { questionnaireActivity.intentHasPopulationResources(intent) }
  }

  @Test
  fun testFragmentQuestionnaireSubmitHandlesQuestionnaireSubmit() {

    every { questionnaireFragment.getQuestionnaireActivity() } returns questionnaireActivity
    every { questionnaireFragment.getQuestionnaireActivity().getQuestionnaireConfig() } returns
      QuestionnaireConfig(id = "123")
    every { questionnaireFragment.getQuestionnaireActivity().getQuestionnaireObject() } returns
      Questionnaire()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireFragment.getQuestionnaireActivity().finish() } just runs
    every { questionnaireFragment.getQuestionnaireActivity().handleQuestionnaireSubmit() } just runs

    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireFragment.getQuestionnaireActivity().handleQuestionnaireSubmit() }
  }

  @Test
  fun testFragmentQuestionnaireSubmitWithReadOnlyModeFinishesActivity() {
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireFragment.getQuestionnaireActivity().finish() } just runs
    every {
      questionnaireFragment.getQuestionnaireActivity().getQuestionnaireConfig().type.isReadOnly()
    } returns true

    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireFragment.getQuestionnaireActivity().finish() }
  }
  @Test
  fun testFragmentQuestionnaireSubmitWithExperimentalModeFinishesActivity() {
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()
    every { questionnaireFragment.getQuestionnaireActivity().finish() } just runs
    every {
      questionnaireFragment.getQuestionnaireActivity().getQuestionnaireObject().experimental
    } returns true
    every { questionnaireFragment.getQuestionnaireActivity().getQuestionnaireConfig() } returns
      QuestionnaireConfig(id = "123")

    questionnaireFragment.setFragmentResult(QuestionnaireFragment.SUBMIT_REQUEST_KEY, Bundle.EMPTY)

    verify { questionnaireFragment.getQuestionnaireActivity().finish() }
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
