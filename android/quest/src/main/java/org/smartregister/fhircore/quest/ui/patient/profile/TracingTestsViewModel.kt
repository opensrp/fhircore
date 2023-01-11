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
import android.content.Intent
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
import com.google.android.fhir.sync.State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HomeTracingRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.PhoneTracingRegisterDao
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.repository.TracingTaskDao
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.TracingUtil
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper
import timber.log.Timber
import java.util.Date

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
  val fhirEngine: FhirEngine,
  val dispatcherProvider: DefaultDispatcherProvider,
  val homeTracingRegisterDao: HomeTracingRegisterDao,
  val phoneTracingRegisterDao: PhoneTracingRegisterDao,
  val tracingUtil: TracingUtil
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

  private val _activeTracingTasksMutableStateFlow =
    MutableStateFlow(listOf<Triple<String, Int, TracingTaskReasonUiRep>>())
  val activeTracingTasksFlow: StateFlow<List<Triple<String, Int, TracingTaskReasonUiRep>>> =
    _activeTracingTasksMutableStateFlow.asStateFlow()

  var patientProfileData: ProfileData? = null

  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  val hasTracing = MutableLiveData(false)

  val tracingHomeCoding: Coding = Coding("https://d-tree.org", "home-tracing", "Home Tracing")
  val tracingPhoneCoding: Coding = Coding("https://d-tree.org", "phone-tracing", "Phone Tracing")

  init {
    fetchPatientProfileData()
    fetchCurrentlyActiveReasons()
    checkIfOnTracing()
    val syncStateListener =
      object : OnSyncListener {
        override fun onSync(state: State) {
          val isStateCompleted = state is State.Failed || state is State.Finished
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

  fun open(context: Context, item: TestItem) {
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

  fun fetchCurrentlyActiveReasons() {
    if (patientId.isBlank()) return
    viewModelScope.launch {
      val tasks = withContext(dispatcherProvider.io()){
       val patient = fhirEngine.loadResource<Patient>(patientId)!!
        val homeTracingTasks = (homeTracingRegisterDao as TracingTaskDao).loadValidTracingTasks(patient)
        val phoneTracingTasks = (phoneTracingRegisterDao as TracingTaskDao).loadValidTracingTasks(patient)
        return@withContext homeTracingTasks + phoneTracingTasks
      }

      withContext(dispatcherProvider.main()) {
        _activeTracingTasksMutableStateFlow.value =
          tasks
            .map { task ->
              val reason =
                task
                  .reasonCode
                  .coding
                  .map { coding -> coding.display ?: coding.code }
                  .ifEmpty { listOf(task.reasonCode.text) }
                  .first()
              val reasonCode = task.reasonCode.coding.random().code
              TracingTaskReasonUiRep(task.logicalId, reasonText = reason, removingQuestionnaire = reasonRemovalQuestionnaire[reasonCode])
            }
            .groupBy { it.reasonText }
            .map {
              val sample = it.value.first()
              Triple(it.key, it.value.size, sample)
            }
      }
    }
  }

  fun removeTracingTask(context: Context, tracingTaskUi: TracingTaskReasonUiRep) {
    context.startActivity(Intent(context, QuestionnaireActivity::class.java)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          formName = tracingTaskUi.removingQuestionnaire!!,
          populationResources = patientProfileViewData.value.populationResources,
          clientIdentifier = patientId,
          questionnaireType = QuestionnaireType.EDIT,
          selectedTracingTask = tracingTaskUi.id
        )))
  }

  fun currentDate() = tracingUtil.getUpperLimitDate()

  fun setUpperDate(date: Long) = tracingUtil.setUpperLimitDate(Date(date))

  companion object {
    val testItems =
      listOf(
        TestItem(
          title = "Viral Load",
          questionnaire = "tests/art_client_viral_load_test_results.json"
        ),
        TestItem(
          title = "Dry Blood Samples",
          questionnaire = "tests/exposed_infant_hiv_test_and_results.json"
        ),
        TestItem(
          title = "D and H VL",
          questionnaire = "tests/art_client_welcome_service_high_or_detectable_viral_load.json"
        ),
        TestItem(
          title = "Next Appointment",
          questionnaire = "tests/contact_and_community_positive_hiv_test_and_next_appointment.json"
        ),
        TestItem(title = "Finish visit test", questionnaire = "tests/patient-finish-visit.json")
      )
    val reasonRemovalQuestionnaire = mapOf(
      "linkage" to "tests/patient-finish-visit.json",
      "interrupt-treat" to "tests/patient-finish-visit.json",
      "miss-appt" to "tests/patient-finish-visit.json",
      "miss-routine" to "tests/patient-finish-visit.json",
      "dual-referral" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json",
      "contact-referral" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json",
      "provider-referral" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json",
      "miss-clinic-appt" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json",
      "frs-stk" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json",
      "frs-no-stk" to "tests/contact_and_community_positive_hiv_test_and_next_appointment.json"
    )
  }
}

data class TestItem(val title: String, val questionnaire: String)

data class TracingTaskReasonUiRep(val id: String, val reasonText: String, val removingQuestionnaire: String?)
