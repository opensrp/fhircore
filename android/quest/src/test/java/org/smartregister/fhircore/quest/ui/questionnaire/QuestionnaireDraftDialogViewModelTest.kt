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
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.AuditEvent
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.ContentCache
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireDraftDialogViewModel.Companion.AUDIT_EVENT_CODE
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireDraftDialogViewModel.Companion.AUDIT_EVENT_DISPLAY
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireDraftDialogViewModel.Companion.AUDIT_EVENT_SYSTEM

@HiltAndroidTest
class QuestionnaireDraftDialogViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var rulesExecutor: RulesExecutor

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var parser: IParser

  @Inject lateinit var contentCache: ContentCache

  private lateinit var questionnaireDraftDialogViewModel: QuestionnaireDraftDialogViewModel
  lateinit var defaultRepository: DefaultRepository
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private val context: Application = ApplicationProvider.getApplicationContext()
  private val configRulesExecutor: ConfigRulesExecutor = mockk()
  lateinit var questionnaireConfig: QuestionnaireConfig
  lateinit var questionnaireResponse: QuestionnaireResponse
  private val practitionerId = "practitioner-id-1"

  @Before
  @ExperimentalCoroutinesApi
  fun setUp() {
    hiltRule.inject()
    // Write practitioner and organization to shared preferences
    sharedPreferencesHelper.write(
      SharedPreferenceKey.PRACTITIONER_ID.name,
      practitionerId,
    )
    defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          configService = configService,
          configRulesExecutor = configRulesExecutor,
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
          context = context,
          contentCache = contentCache,
        ),
      )
    questionnaireDraftDialogViewModel =
      spyk(
        QuestionnaireDraftDialogViewModel(
          defaultRepository = defaultRepository,
          sharedPreferencesHelper = sharedPreferencesHelper,
        ),
      )

    questionnaireConfig =
      QuestionnaireConfig(
        id = "dc-clinic-medicines",
        resourceType = ResourceType.Patient,
        resourceIdentifier = "Patient-id-1",
      )
    questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "qr-id-1"
        status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
        subject = "Patient-id-1".asReference(ResourceType.Patient)
        questionnaire = "Questionnaire/dc-clinic-medicines"
      }
  }

  @Test
  fun testDeleteDraftUpdateQuestionnaireResponseStatusToStoppedAndAuditEvent() {
    runTest(timeout = 90.seconds) {
      // add QR to db
      fhirEngine.create(questionnaireResponse)
      val savedDraft = fhirEngine.get<QuestionnaireResponse>("qr-id-1")
      assertEquals("QuestionnaireResponse/qr-id-1", savedDraft.id)
      assertEquals("Patient/Patient-id-1", savedDraft.subject.reference)
      assertEquals("Questionnaire/dc-clinic-medicines", savedDraft.questionnaire)
      assertEquals("in-progress", savedDraft.status.toCode())

      runBlocking {
        questionnaireDraftDialogViewModel.deleteDraft(questionnaireConfig = questionnaireConfig)
      }

      val deletedDraft = fhirEngine.get<QuestionnaireResponse>("qr-id-1")
      assertEquals("QuestionnaireResponse/qr-id-1", deletedDraft.id)
      assertEquals("Patient/Patient-id-1", deletedDraft.subject.reference)
      assertEquals("Questionnaire/dc-clinic-medicines", deletedDraft.questionnaire)
      assertEquals("stopped", deletedDraft.status.toCode())

      val search =
        Search(ResourceType.AuditEvent).apply {
          filter(
            AuditEvent.SOURCE,
            { value = "Patient-id-1".asReference(ResourceType.Patient).reference },
          )
          filter(
            AuditEvent.TYPE,
            { value = of("delete_draft") },
          )
        }

      val createdAuditEventList = defaultRepository.search<AuditEvent>(search)
      assertNotNull(createdAuditEventList)
      assertEquals(
        "QuestionnaireResponse/qr-id-1",
        createdAuditEventList[0].entity[0].what.reference,
      )
      assertEquals(
        "Practitioner/practitioner-id-1",
        createdAuditEventList[0].agent[0].who.reference,
      )
      assertEquals("Patient/Patient-id-1", createdAuditEventList[0].source.observer.reference)
    }
  }

  @Test
  fun testCreateDeleteDraftFlag() {
    val auditEvent =
      questionnaireDraftDialogViewModel.createDeleteDraftAuditEvent(
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = questionnaireResponse,
      )

    assertEquals("Patient/Patient-id-1", auditEvent.source.observer.reference)
    assertEquals("Practitioner/practitioner-id-1", auditEvent.agent[0].who.reference)
    assertEquals(AUDIT_EVENT_SYSTEM, auditEvent.type.system)
    assertEquals(AUDIT_EVENT_CODE, auditEvent.type.code)
    assertEquals(AUDIT_EVENT_DISPLAY, auditEvent.type.display)
  }
}
