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

package org.dtree.fhircore.dataclerk.ui.patient

import android.content.Context
import android.icu.text.DateFormat
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dtree.fhircore.dataclerk.R
import org.dtree.fhircore.dataclerk.ui.main.AppDataStore
import org.dtree.fhircore.dataclerk.util.getFormattedAge
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType

@HiltViewModel
class PatientViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val appDataStore: AppDataStore,
  @ApplicationContext val context: Context
) : ViewModel() {
  private val patientId = savedStateHandle.get<String>("patientId") ?: ""
  val screenState = MutableStateFlow<PatientDetailScreenState>(PatientDetailScreenState.Loading)
  val resourceMapStatus: MutableState<Map<String, MutableStateFlow<ResourcePropertyState>>> =
    mutableStateOf(mapOf())
  init {
    fetchPatient()
  }

  fun fetchPatient() {
    viewModelScope.launch {
      try {
        screenState.emit(PatientDetailScreenState.Loading)
        val data = mutableListOf<PatientDetailData>()
        val patient = appDataStore.getPatient(patientId)
        val hashList = mutableListOf<String>()
        patient.let { patientItem ->
          data.add(PatientDetailOverview(patientItem, firstInGroup = true))
          data.add(PatientDetailProperty(PatientProperty("HCC/ArtNumber", patientItem.id)))
          data.add(
            PatientDetailProperty(
              PatientProperty(getString(R.string.patient_property_mobile), patientItem.phone)
            )
          )
          data.add(
            PatientDetailProperty(
              PatientProperty(
                getString(R.string.patient_property_dob),
                patientItem.dob?.localizedString ?: ""
              )
            )
          )
          data.add(
            PatientDetailProperty(
              PatientProperty(
                getString(R.string.patient_property_age),
                getFormattedAge(patient, context.resources)
              )
            )
          )
          data.add(
            PatientDetailProperty(
              PatientProperty(
                getString(R.string.patient_property_gender),
                patientItem.gender.replaceFirstChar {
                  if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
              ),
              lastInGroup = true
            )
          )
          val address = patientItem.addressData.fullAddress
          data.add(
            PatientDetailProperty(
              PatientProperty(
                getString(R.string.patient_property_address),
                address.ifBlank { "N/A" }
              )
            )
          )
          data.add(
            PatientReferenceProperty(PatientProperty("CHW Assigned", patientItem.chwAssigned))
          )
          if (patientItem.chwAssigned.isNotBlank()) {
            hashList.add(patientItem.chwAssigned)
          }
        }
        screenState.emit(PatientDetailScreenState.Success(patient, data))
        fetchResources(hashList)
      } catch (e: Exception) {
        screenState.emit(PatientDetailScreenState.Error(e.message ?: "Error"))
      }
    }
  }

  private fun fetchResources(resourceIds: List<String>) {
    viewModelScope.launch { resourceIds.forEach { fetchResource(it) } }
  }

  private suspend fun fetchResource(resourceId: String) {
    viewModelScope.launch {
      try {
        resourceMapStatus.value =
          resourceMapStatus.value.toMutableMap().apply {
            this[resourceId] = MutableStateFlow(ResourcePropertyState.Loading)
          }
        val resource = appDataStore.getResource(resourceId)
        resourceMapStatus.value =
          resourceMapStatus.value.toMutableMap().apply {
            this[resourceId] = MutableStateFlow(ResourcePropertyState.Success(resource))
          }
      } catch (e: Exception) {
        resourceMapStatus.value =
          resourceMapStatus.value.toMutableMap().apply {
            this[resourceId] = MutableStateFlow(ResourcePropertyState.Error(e.message ?: "Error"))
          }
      }
    }
  }

  private fun getString(resId: Int) = context.resources.getString(resId)
  fun editPatient(context: Context) {
    QuestionnaireActivity.launchQuestionnaire(
      context = context,
      questionnaireId = EDIT_PROFILE_FORM,
      clientIdentifier = patientId,
      questionnaireType = QuestionnaireType.EDIT
    )
  }

  companion object {
    const val EDIT_PROFILE_FORM = "edit-patient-profile"
  }
}

private fun isAndroidIcuSupported() = true

val LocalDate.localizedString: String
  get() {
    val date = Date.from(atStartOfDay(ZoneId.systemDefault())?.toInstant())
    return if (isAndroidIcuSupported()) DateFormat.getDateInstance(DateFormat.DEFAULT).format(date)
    else SimpleDateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date)
  }
