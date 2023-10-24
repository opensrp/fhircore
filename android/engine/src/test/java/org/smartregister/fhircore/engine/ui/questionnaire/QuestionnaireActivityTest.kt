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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator.checkQuestionnaireResponse
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
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
import org.junit.Test.None
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.di.AnalyticsModule
import org.smartregister.fhircore.engine.di.CoreModule
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.trace.FakePerformanceReporter
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.distinctifyLinkId
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString

@UninstallModules(AnalyticsModule::class, CoreModule::class)
@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class QuestionnaireActivityTest : ActivityRobolectricTest() {

  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireFragment: QuestionnaireFragment

  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  val dispatcherProvider: DispatcherProvider = spyk(DefaultDispatcherProvider())

  private val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  @BindValue @JvmField val performanceReporter: PerformanceReporter = FakePerformanceReporter()

  @BindValue val accountManager = mockk<AccountManager>()

  @BindValue val syncBroadcaster = mockk<SyncBroadcaster>()

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(
      QuestionnaireViewModel(
        fhirEngine = mockk(),
        defaultRepository = mockk { coEvery { addOrUpdate(true, any()) } just runs },
        configurationRegistry = mockk(),
        transformSupportServices = mockk(),
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = mockk(),
        libraryEvaluatorProvider = { mockk<LibraryEvaluator>() },
        tracer = FakePerformanceReporter()
      )
    )

  @Before
  fun setUp() {
    // TODO Proper set up
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putStringArrayListExtra(QuestionnaireActivity.QUESTIONNAIRE_LAUNCH_CONTEXT, arrayListOf())
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "1234")
      }

    every { syncBroadcaster.runSync(any()) } just runs
    coEvery { questionnaireViewModel.libraryEvaluatorProvider.get().initialize() } just runs

    val questionnaireConfig = QuestionnaireConfig("form", "title", "form-id")
    coEvery { questionnaireViewModel.getQuestionnaireConfig(any(), any()) } returns
      questionnaireConfig
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns Questionnaire()
    coEvery { questionnaireViewModel.generateQuestionnaireResponse(any(), any()) } returns
      QuestionnaireResponse()

    val questionnaireString = parser.encodeResourceToString(Questionnaire())

    questionnaireFragment = spyk()

    questionnaireFragment.apply { arguments = bundleOf(Pair("questionnaire", questionnaireString)) }

    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(SharedPreferencesHelper)
    unmockkObject(AssetUtil)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(
      questionnaireActivity.supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
    )
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
        clientIdentifier = "1234",
        formName = "my-form",
        questionnaireType = QuestionnaireType.READ_ONLY,
        questionnaireResponse = questionnaireResponse,
        populationResources = populationResources
      )
    Assert.assertEquals("my-form", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      "1234",
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
    Assert.assertEquals(
      QuestionnaireType.READ_ONLY.name,
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE)
    )
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
  }

  @Test
  fun testGeneratedQuestionnaireResponseWithMultipleLinkIdNotValid() = runTest {
    val questionnaireViewModel2 = spyk(questionnaireViewModel)
    val questionnaire =
      Questionnaire().apply {
        addItem().apply {
          linkId = "page-1"
          type = Questionnaire.QuestionnaireItemType.GROUP
          text = "Page 1"
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
            setValue(
              CodeableConcept(
                Coding("http://hl7.org/fhir/questionnaire-item-control", "page", "Page")
              )
            )
          }
          addItem().apply {
            linkId = "phone-info"
            type = Questionnaire.QuestionnaireItemType.GROUP
            text = "Phone Info"
            addItem().apply {
              linkId = "phone-type"
              text = "Phone Type"
              type = Questionnaire.QuestionnaireItemType.CHOICE
              addAnswerOption().apply { value = StringType("Mobile") }
              addAnswerOption().apply { value = StringType("Office") }
            }

            addItem().apply {
              linkId = "phone-value-1"
              text = "Phone Number"
              type = Questionnaire.QuestionnaireItemType.STRING
              addEnableWhen().apply {
                question = "phone-type"
                operator = Questionnaire.QuestionnaireItemOperator.EQUAL
                answer = StringType("Office")
              }
            }

            addItem().apply {
              linkId = "phone-value-1"
              text = "Phone Number"
              type = Questionnaire.QuestionnaireItemType.STRING
              addEnableWhen().apply {
                question = "phone-type"
                operator = Questionnaire.QuestionnaireItemOperator.EQUAL
                answer = StringType("Mobile")
              }
            }
          }
        }
      }
    val populationIntent =
      Intent().apply {
        putStringArrayListExtra(
          QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES,
          arrayListOf(Patient().encodeResourceToString())
        )
      }
    val questionnaireResponse =
      questionnaireViewModel2.generateQuestionnaireResponse(questionnaire, populationIntent)
    assertFailsWith<IllegalArgumentException>(
      message = "Multiple answers for non-repeat questionnaire item phone-value-1"
    ) { checkQuestionnaireResponse(questionnaire, questionnaireResponse) }
  }

  @Test(expected = None::class)
  fun testGeneratedQuestionnaireResponseWithDistinctifyLinkIdValid() = runTest {
    val questionnaireViewModel2 = spyk(questionnaireViewModel)
    val questionnaire =
      Questionnaire().apply {
        addItem().apply {
          linkId = "page-1"
          type = Questionnaire.QuestionnaireItemType.GROUP
          text = "Page 1"
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
            setValue(
              CodeableConcept(
                Coding("http://hl7.org/fhir/questionnaire-item-control", "page", "Page")
              )
            )
          }
          addItem().apply {
            linkId = "phone-info"
            type = Questionnaire.QuestionnaireItemType.GROUP
            text = "Phone Info"
            addItem().apply {
              linkId = "phone-type"
              text = "Phone Type"
              type = Questionnaire.QuestionnaireItemType.CHOICE
              addAnswerOption().apply { value = StringType("Mobile") }
              addAnswerOption().apply { value = StringType("Office") }
            }

            addItem().apply {
              linkId = "phone-value-1"
              text = "Phone Number"
              type = Questionnaire.QuestionnaireItemType.STRING
              addEnableWhen().apply {
                question = "phone-type"
                operator = Questionnaire.QuestionnaireItemOperator.EQUAL
                answer = StringType("Office")
              }
            }

            addItem().apply {
              linkId = "phone-value-1"
              text = "Phone Number"
              type = Questionnaire.QuestionnaireItemType.STRING
              addEnableWhen().apply {
                question = "phone-type"
                operator = Questionnaire.QuestionnaireItemOperator.EQUAL
                answer = StringType("Mobile")
              }
            }
          }
        }
      }
    val populationIntent =
      Intent().apply {
        putStringArrayListExtra(
          QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES,
          arrayListOf(Patient().encodeResourceToString())
        )
      }
    val questionnaireResponse =
      questionnaireViewModel2.generateQuestionnaireResponse(questionnaire, populationIntent)
    questionnaireResponse.distinctifyLinkId()
    checkQuestionnaireResponse(questionnaire, questionnaireResponse)
  }

  @Test
  fun testReadOnlyIntentShouldBeReadToReadOnlyFlag() {
    Assert.assertFalse(questionnaireActivity.questionnaireType.isReadOnly())

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE, QuestionnaireType.READ_ONLY.name)
      }

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()

    Assert.assertTrue(questionnaireActivity.questionnaireType.isReadOnly())
  }

  @Test
  fun testReadOnlyIntentShouldChangeSaveButtonToDone() {
    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE, QuestionnaireType.READ_ONLY.name)
        putExtra(
          QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
          QuestionnaireResponse().encodeResourceToString()
        )
      }

    val questionnaireString = parser.encodeResourceToString(Questionnaire())

    val questionnaireFragment = spyk<QuestionnaireFragment>()
    questionnaireFragment.apply { arguments = bundleOf(Pair("questionnaire", questionnaireString)) }

    every { questionnaireFragment.getQuestionnaireResponse() } returns QuestionnaireResponse()

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
    questionnaireActivity.supportFragmentManager.commitNow {
      add(questionnaireFragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }

    Assert.assertEquals("Done", questionnaireActivity.submitButtonText())
  }

  @Test
  fun `activity finishes when loadQuestionnaireAndConfig fails with loadQuestionnaireAndConfig from viewmodel`() =
      runTest {
    coEvery {
      questionnaireViewModel.getQuestionnaireConfigPair(questionnaireActivity, any(), any())
    } throws QuestionnaireNotFoundException("unknown_form")
    questionnaireActivity.loadQuestionnaireAndConfig("unknown_form")
    Assert.assertTrue(questionnaireActivity.isFinishing.or(questionnaireActivity.isDestroyed))
  }

  @Test
  fun testOnBackPressedShouldShowAlert() {
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
  fun testOnBackPressedShouldCallFinishWhenInReadOnlyMode() {
    val qActivity = spyk(questionnaireActivity)
    ReflectionHelpers.setField(qActivity, "questionnaireType", QuestionnaireType.READ_ONLY)
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
        intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_GROUP_KEY),
        any(),
        any(),
        any()
      )
    }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowProgressAndCallExtractAndSaveResources() {
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", Questionnaire())
    questionnaireActivity.handleQuestionnaireSubmit()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.form_progress_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )

    verify(timeout = 2000) {
      questionnaireViewModel.extractAndSaveResources(
        any(),
        any(),
        intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_GROUP_KEY),
        any(),
        any(),
        any()
      )
    }
  }

  @Test
  fun testHandleQuestionnaireSubmitShouldShowErrorAlertOnInvalidData() = runTest {
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
      questionnaireViewModel.extractAndSaveResources(
        any(),
        any(),
        intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_GROUP_KEY),
        any(),
        any(),
        any()
      )
    }
  }

  @Test
  fun testOnClickSaveButtonShouldShowSubmitConfirmationAlert() {
    ReflectionHelpers.setField(
      questionnaireActivity,
      "questionnaire",
      Questionnaire().apply { experimental = false }
    )

    questionnaireActivity.onSubmitRequestResult()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_submit_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testOnClickSaveWithExperimentalButtonShouldShowTestOnlyConfirmationAlert() {
    ReflectionHelpers.setField(
      questionnaireActivity,
      "questionnaire",
      Questionnaire().apply { experimental = true }
    )

    questionnaireActivity.onSubmitRequestResult()

    val dialog = shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertEquals(
      getString(R.string.questionnaire_alert_test_only_message),
      alertDialog.findViewById<TextView>(R.id.tv_alert_message)!!.text
    )
  }

  @Test
  fun testOnClickEditButtonShouldSetEditModeToTrue() = runBlockingTest {
    val questionnaire = Questionnaire().apply { experimental = false }
    ReflectionHelpers.setField(questionnaireActivity, "questionnaire", questionnaire)
    Assert.assertFalse(questionnaireActivity.questionnaireType.isEditMode())

    questionnaireActivity.onClick(questionnaireActivity.findViewById(R.id.btn_edit_qr))

    Assert.assertTrue(questionnaireActivity.questionnaireType.isEditMode())
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
  fun testPostSaveSuccessfulShouldFinishActivity() {
    questionnaireActivity.postSaveSuccessful(QuestionnaireResponse())

    Assert.assertTrue(questionnaireActivity.isFinishing)
  }

  @Test
  fun testPostSaveSuccessfulWithExtractionMessageShouldShowAlert() = runTest {
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
  fun testFinishActivity() {
    questionnaireActivity.finishActivity(QuestionnaireResponse(), null)

    Assert.assertTrue(questionnaireActivity.isFinishing)
  }

  @Test
  fun testFinishActivityWithEncounter() {
    val spiedActivity = spyk(questionnaireActivity)
    spiedActivity.finishActivity(
      QuestionnaireResponse(),
      listOf(Encounter().apply { status = Encounter.EncounterStatus.FINISHED })
    )

    verify {
      spiedActivity.setResult(
        Activity.RESULT_OK,
        withArg {
          Assert.assertEquals(
            Encounter.EncounterStatus.FINISHED.toCode(),
            it.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_RES_ENCOUNTER)
          )
        }
      )
    }
    Assert.assertTrue(spiedActivity.isFinishing)
  }

  /** Launch Questionnaire tests */
  @Test
  fun launchQuestionnaireCallsStartActivity() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val ctx = mockk<Context>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivity(any()) } just runs
    QuestionnaireActivity.launchQuestionnaire(
      ctx,
      questionnaireId = "testQuestionnaire",
      clientIdentifier = null,
      populationResources = arrayListOf()
    )

    verify { ctx.startActivity(any()) }
  }

  @Test
  fun launchQuestionnaireForResultCallsStartActivityForResultWithRequestCode() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs
    QuestionnaireActivity.launchQuestionnaireForResult(
      ctx,
      questionnaireId = "testQuestionnaire",
      clientIdentifier = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivityForResult(any(), withArg { Assert.assertEquals(0, it) }) }
  }

  @Test
  fun launchQuestionnaireForResultCallsStartActivityForResultWithNullLaunchContexts() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs
    QuestionnaireActivity.launchQuestionnaireForResult(
      ctx,
      questionnaireId = "testQuestionnaire",
      launchContexts = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivityForResult(any(), withArg { Assert.assertEquals(0, it) }) }
  }

  @Test
  fun launchQuestionnaireCallsStartActivityForResultWithNullLaunchContexts() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val ctx = spyk<Activity>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivity(any()) } just runs
    QuestionnaireActivity.launchQuestionnaire(
      ctx,
      questionnaireId = "testQuestionnaire",
      launchContexts = null,
      populationResources = arrayListOf()
    )
    verify { ctx.startActivity(any()) }
  }

  @Test
  fun launchQuestionnaire_StartActivityCalledWithAllParameters() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val questionnaireId = "questionnaire_id"
    val clientIdentifier = "client_identifier"
    val groupIdentifier = "group_identifier"
    val intentBundle = Bundle.EMPTY
    val questionnaireType = QuestionnaireType.DEFAULT
    val launchContexts = mockk<ArrayList<Resource>>(relaxed = true)
    val populationResources = mockk<ArrayList<Resource>>(relaxed = true)

    val ctx = spyk<Activity>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivity(any()) } just runs

    QuestionnaireActivity.launchQuestionnaire(
      ctx,
      questionnaireId = questionnaireId,
      clientIdentifier = clientIdentifier,
      groupIdentifier = groupIdentifier,
      intentBundle = intentBundle,
      questionnaireType = questionnaireType,
      launchContexts = launchContexts,
      populationResources = populationResources
    )

    val expectedIntent =
      Intent(ctx, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = clientIdentifier,
            groupIdentifier = groupIdentifier,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            launchContexts = launchContexts,
            populationResources = populationResources
          )
        )

    verify {
      ctx.startActivity(
        withArg {
          Assert.assertEquals(
            expectedIntent.getStringExtra("clientIdentifier"),
            it.getStringExtra("clientIdentifier")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("groupIdentifier"),
            it.getStringExtra("groupIdentifier")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("formName"),
            it.getStringExtra("formName")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("questionnaireType"),
            it.getStringExtra("questionnaireType")
          )
          Assert.assertEquals(
            expectedIntent.getStringArrayListExtra("launchContexts"),
            it.getStringArrayListExtra("launchContexts")
          )
          Assert.assertEquals(
            expectedIntent.getStringArrayListExtra("populationResources"),
            it.getStringArrayListExtra("populationResources")
          )
        }
      )
    }
  }

  @Test
  fun launchQuestionnaireCallsStartActivityForResultWithAllParameters() {
    val appContext = ApplicationProvider.getApplicationContext<Application>()
    val questionnaireId = "questionnaire_id"
    val clientIdentifier = "client_identifier"
    val backReference = "back_reference"
    val intentBundle = Bundle.EMPTY
    val questionnaireType = QuestionnaireType.DEFAULT
    val launchContexts = mockk<ArrayList<Resource>>(relaxed = true)
    val populationResources = mockk<ArrayList<Resource>>(relaxed = true)

    val ctx = spyk<Activity>()
    every { ctx.packageName } returns appContext.packageName
    every { ctx.startActivityForResult(any(), any()) } just runs

    QuestionnaireActivity.launchQuestionnaireForResult(
      ctx,
      questionnaireId = questionnaireId,
      clientIdentifier = clientIdentifier,
      questionnaireType = questionnaireType,
      backReference = backReference,
      intentBundle = intentBundle,
      launchContexts = launchContexts,
      populationResources = populationResources
    )

    val expectedIntent =
      Intent(ctx, QuestionnaireActivity::class.java)
        .putExtras(intentBundle)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = clientIdentifier,
            backReference = backReference,
            formName = questionnaireId,
            questionnaireType = questionnaireType,
            launchContexts = launchContexts,
            populationResources = populationResources
          ),
        )

    verify {
      ctx.startActivityForResult(
        withArg {
          Assert.assertEquals(
            expectedIntent.getStringExtra("clientIdentifier"),
            it.getStringExtra("clientIdentifier")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("backReference"),
            it.getStringExtra("backReference")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("formName"),
            it.getStringExtra("formName")
          )
          Assert.assertEquals(
            expectedIntent.getStringExtra("questionnaireType"),
            it.getStringExtra("questionnaireType")
          )
          Assert.assertEquals(
            expectedIntent.getStringArrayListExtra("launchContexts"),
            it.getStringArrayListExtra("launchContexts")
          )
          Assert.assertEquals(
            expectedIntent.getStringArrayListExtra("populationResources"),
            it.getStringArrayListExtra("populationResources")
          )
        },
        0
      )
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
    }
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }
}
