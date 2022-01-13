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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Observation
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.quest.configuration.view.DetailViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemCell
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemRow
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import org.smartregister.fhircore.quest.util.getSearchResults
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
          configClassification = QuestConfigClassification.TEST_RESULT_DETAIL_VIEW
        )

      val dataItem = DetailsViewItem()
      dataItem.label = config.label

      config.rows.forEach {
        val row = DetailsViewItemRow()

        it.filters.forEach { f ->
          val value =
            when (f.resourceType) {
              Enumerations.ResourceType.CONDITION ->
                getCondition(encounter, f)?.code?.codingFirstRep
              Enumerations.ResourceType.OBSERVATION -> getObservation(encounter, f)?.value
              else -> null
            }

          row.cells.add(DetailsViewItemCell(value, f))
        }

        dataItem.rows.add(row)
      }

      Timber.i(dataItem.rows.toString())

      _detailsViewItem.postValue(dataItem)
    }
  }

  suspend fun getCondition(encounter: Encounter, filter: Filter): Condition? {
    return getSearchResults<Condition>(
        encounter.referenceValue(),
        Condition.ENCOUNTER,
        filter,
        patientRepository.fhirEngine
      )
      .firstOrNull()
  }

  suspend fun getObservation(encounter: Encounter, filter: Filter): Observation? {
    return getSearchResults<Observation>(
        encounter.referenceValue(),
        Observation.ENCOUNTER,
        filter,
        patientRepository.fhirEngine
      )
      .firstOrNull()
  }
}
