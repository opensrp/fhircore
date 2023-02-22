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

package org.smartregister.fhircore.quest.ui.patient.profile

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.delete
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper
import timber.log.Timber

@HiltViewModel
class TracingTestsViewModel
@Inject
constructor(
  syncBroadcaster: SyncBroadcaster,
  savedStateHandle: SavedStateHandle,
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val profileViewDataMapper: ProfileViewDataMapper,
  val registerViewDataMapper: RegisterViewDataMapper,
  val fhirEngine: FhirEngine
) : ViewModel() {
  val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.DEFAULT
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  val familyId = savedStateHandle.get<String>(NavigationArg.FAMILY_ID)

  private val _patientProfileViewDataFlow =
    MutableStateFlow(ProfileViewData.PatientProfileViewData())
  val patientProfileViewData: StateFlow<ProfileViewData.PatientProfileViewData>
    get() = _patientProfileViewDataFlow.asStateFlow()

  var patientProfileData: ProfileData? = null

  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  val hasTracing = MutableLiveData(false)

  val tracingHomeCoding: Coding = Coding("https://d-tree.org", "home-tracing", "Home Tracing")
  val tracingPhoneCoding: Coding = Coding("https://d-tree.org", "phone-tracing", "Phone Tracing")

  init {
    fetchPatientProfileData()
    checkIfOnTracing()
    val syncStateListener =
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          val isStateCompleted = state is SyncJobStatus.Failed || state is SyncJobStatus.Finished
          if (isStateCompleted) checkIfOnTracing()
        }
      }
    syncBroadcaster.registerSyncListener(syncStateListener, viewModelScope)
  }

  fun fetchPatientProfileData() {
    if (patientId.isNotEmpty()) {
      viewModelScope.launch {
        patientRegisterRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)
          ?.let {
            patientProfileData = it
            _patientProfileViewDataFlow.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.PatientProfileViewData
          }
      }
    }
  }

  fun open(context: Context, item: TestItem.QuestItem) {
    val profile = patientProfileViewData.value

    context.launchQuestionnaire<QuestionnaireActivity>(
      questionnaireId = item.questionnaire,
      populationResources = profile.populationResources,
      clientIdentifier = patientId,
      questionnaireType = QuestionnaireType.EDIT
    )
  }

  fun checkIfOnTracing() {
    viewModelScope.launch {
      try {
        val patientRef = "Patient/$patientId"
        val valuesHome =
          fhirEngine.search<Task> {
            filter(
              Task.CODE,
              {
                value =
                  of(
                    CodeableConcept()
                      .addCoding(
                        Coding("http://snomed.info/sct", "225368008", "Contact tracing (procedure)")
                      )
                  )
              }
            )
            filter(Task.SUBJECT, { value = patientRef })
          }
        valuesHome.forEach {
          val data = jsonParser.encodeResourceToString(it)
          Timber.e(data)
        }
        hasTracing.value = valuesHome.isNotEmpty() // || valuesPhone.isNotEmpty()
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  fun updateUserWithTracing(isHomeTracing: Boolean) {
    try {
      viewModelScope.launch {
        val data =
          """
            {
        "resourceType": "Task",
        "status": "ready",
        "intent": "plan",
        "priority": "routine",
        "code": {
          "coding": [
            {
              "system": "http://snomed.info/sct",
              "code": "225368008",
              "display": "Contact tracing (procedure)"
            }
          ],
          "text": "Contact Tracing"
        },
        "executionPeriod": { "start": "2022-11-22T09:51:57+02:00" },
        "authoredOn": "2022-11-22T09:51:57+02:00",
        "lastModified": "2022-11-22T09:51:57+02:00",
        "owner": {
          "reference": "Practitioner/649b723c-28f3-4f5f-8fcf-28405b57a1ec"
        },
        "reasonCode": {
          "coding": [
            {
              "system": "https://d-tree.org",
              "code": "missing-vl",
              "display": "Missing Viral Load"
            }
          ],
          "text": "Missing VL"
        },
        "reasonReference": {
          "reference": "Questionnaire/art-client-viral-load-test-results"
        },
        "for": { "reference": "Patient/$patientId" }
      }
          """.trimIndent()

        val task = jsonParser.parseResource(Task::class.java, data)
        task.description =
          if (isHomeTracing) "HIV Contact Tracing via home visit"
          else "HIV Contact Tracing via phone"
        task.meta.tag.add(
          Coding(
            "https://d-tree.org",
            if (isHomeTracing) "home-tracing" else "phone-tracing",
            if (isHomeTracing) "Home Tracing" else "Phone Tracing"
          )
        )
        val tasks = fhirEngine.create(task)
        val createdTask = fhirEngine.get(ResourceType.Task, tasks.first())
        Timber.i(jsonParser.encodeResourceToString(createdTask))
        checkIfOnTracing()
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun clearAllTracingData() {
    viewModelScope.launch {
      val allData =
        fhirEngine.search<Task> {
          filter(
            Task.CODE,
            {
              value =
                of(
                  CodeableConcept()
                    .addCoding(
                      Coding("http://snomed.info/sct", "225368008", "Contact tracing (procedure)")
                    )
                )
            }
          )
        }
      allData.forEach { fhirEngine.delete<Task>(it.logicalId) }
      checkIfOnTracing()
    }
  }

  companion object {
    val testItems: List<TestItem> =
      listOf(
        TestItem.QuestItem(
          title = "art client viral load test results",
          questionnaire = "tests/art_client_viral_load_test_results.json",
          tracingList = listOf("HVL", "MVl", "IVl"),
          appointmentList = listOf("ICT", "VL")
        ),
        TestItem.QuestItem(
          title = "exposed infant hiv test and results",
          questionnaire = "tests/exposed_infant_hiv_test_and_results.json",
          tracingList = listOf("PDBS", "MDBS", "IDBS"),
          appointmentList = listOf("Milestone")
        ),
        TestItem.QuestItem(
          title = "welcome service high or detectable viral load",
          questionnaire = "tests/art_client_welcome_service_high_or_detectable_viral_load.json",
          tracingList = listOf("-HVL")
        ),
        TestItem.QuestItem(
          title = "hiv test and next appointment",
          questionnaire = "tests/contact_and_community_positive_hiv_test_and_next_appointment.json"
        ),
        TestItem.QuestItem(
          title = "Art Welcome Service",
          questionnaire = "tests/art_client_welcome_service.json",
                appointmentList = listOf("Followup")
        ),
      )
  }
}

sealed class TestItem() {
  data class QuestItem(
    val title: String,
    val questionnaire: String,
    val tracingList: List<String> = listOf(),
    val appointmentList: List<String> = listOf()
  ) : TestItem()
  object DividerItem : TestItem()
}
