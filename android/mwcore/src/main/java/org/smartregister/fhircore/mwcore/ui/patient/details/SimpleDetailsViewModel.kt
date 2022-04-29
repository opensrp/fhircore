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

package org.smartregister.fhircore.mwcore.ui.patient.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.mwcore.configuration.view.DetailViewConfiguration
import org.smartregister.fhircore.mwcore.data.patient.PatientRepository
import org.smartregister.fhircore.mwcore.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.mwcore.data.patient.model.DetailsViewItemCell
import org.smartregister.fhircore.mwcore.data.patient.model.DetailsViewItemRow
import org.smartregister.fhircore.mwcore.util.FhirPathUtil.doesSatisfyFilter
import org.smartregister.fhircore.mwcore.util.FhirPathUtil.getPathValue
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification
import timber.log.Timber

@HiltViewModel
class SimpleDetailsViewModel @Inject constructor(val patientRepository: PatientRepository) :
  ViewModel(), SimpleDetailsDataProvider {

  @VisibleForTesting
  private val _detailsViewItem: MutableLiveData<DetailsViewItem> = MutableLiveData(null)
  override val detailsViewItem: LiveData<DetailsViewItem> = _detailsViewItem

  private val _onBackPressClicked: MutableLiveData<Boolean> = MutableLiveData(false)
  override val onBackPressClicked: LiveData<Boolean> = _onBackPressClicked

  override fun onBackPressed(back: Boolean) {
    _onBackPressClicked.postValue(back)
  }

  fun loadData(encounterId: String) {
    viewModelScope.launch {
      val encounter = patientRepository.loadEncounter(encounterId)
      val config =
        patientRepository.configurationRegistry.retrieveConfiguration<DetailViewConfiguration>(
          configClassification = MwCoreConfigClassification.TEST_RESULT_DETAIL_VIEW
        )

      val dataItem = DetailsViewItem()
      dataItem.label = config.label

      val dataMap = getDataMap(encounter)

      config.rows.forEach {
        val row = DetailsViewItemRow()

        it.filters.forEach { f ->
          // get the required property from pre-loaded resources e.g. CONDITION
          val value =
            dataMap[f.resourceType]
              ?.find { doesSatisfyFilter(it, f) == true }
              ?.getPathValue(f.displayableProperty)

          row.cells.add(DetailsViewItemCell(value, f))
        }

        dataItem.rows.add(row)
      }

      Timber.i(dataItem.rows.toString())

      _detailsViewItem.postValue(dataItem)
    }
  }

  /**
   * we are loading data for an encounter hence it makes most sense to load all relevant data rather
   * than running multiple queries for each cell. This improves screen loading speed we can further
   * scale this when needed. Has data sorted by date in descending order
   */
  suspend fun getDataMap(
    encounter: Encounter
  ): MutableMap<Enumerations.ResourceType, List<Resource>> {
    return mutableMapOf(
      Enumerations.ResourceType.PATIENT to listOf(getPatient(encounter)),
      Enumerations.ResourceType.CONDITION to patientRepository.getCondition(encounter, null),
      Enumerations.ResourceType.OBSERVATION to patientRepository.getObservation(encounter, null),
      Enumerations.ResourceType.MEDICATIONREQUEST to
        patientRepository.getMedicationRequest(encounter, null)
    )
  }

  suspend fun getPatient(encounter: Encounter) =
    patientRepository.fetchDemographics(encounter.subject.extractId())
}
