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
import com.google.android.fhir.search.search
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

  init {
    fetchPatientProfileData()
    checkIfOnTracing()
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
    context.launchQuestionnaire<QuestionnaireActivity>(
      questionnaireId = item.questionnaire,
      clientIdentifier = patientId,
      questionnaireType = QuestionnaireType.EDIT
    )
  }

  fun checkIfOnTracing() {
    viewModelScope.launch {
      try {
        val stuff = fhirEngine.search("Task?code=http://snomed.info/sct|225368008&subject")
        val values =
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
        val tracingTasks =
          stuff.forEach {
            val data = jsonParser.encodeResourceToString(it)
            Timber.e(data)
          }
        Timber.i(tracingTasks.toString())
        hasTracing.value = values.isNotEmpty()
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  fun updateUserWithTracing(isHomeTracing: Boolean = false) {
    try {
      viewModelScope.launch {
        val data =
          if (isHomeTracing) {
            ""
          } else {
            """
            {
        "resourceType": "Task",
        "meta": {
          "tag": [
            {
              "system": "https://d-tree.org",
              "code": "phone-tracing",
              "display": "Phone Tracing"
            }
          ]
        },
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
        "description": "HIV Contact Tracing via phone",
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
          }

        val task = jsonParser.parseResource(Task::class.java, data)
        val tasks = fhirEngine.create(task)
        val createdTask = fhirEngine.get(ResourceType.Task, tasks.first())
        Timber.i(jsonParser.encodeResourceToString(createdTask))
        checkIfOnTracing()
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

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
        )
      )
  }
}

data class TestItem(val title: String, val questionnaire: String)
