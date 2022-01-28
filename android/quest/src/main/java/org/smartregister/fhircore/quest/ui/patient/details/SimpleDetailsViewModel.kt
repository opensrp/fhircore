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
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.quest.configuration.view.DetailViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.isSimilar
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

  private val fhirPathEngine = FHIRPathEngine(SimpleWorkerContext())

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
      Enumerations.ResourceType.CONDITION to getCondition(encounter, null),
      Enumerations.ResourceType.OBSERVATION to getObservation(encounter, null),
      Enumerations.ResourceType.MEDICATIONREQUEST to getMedicationRequest(encounter, null)
    )
  }

  fun Base.getPathValue(path: String) = fhirPathEngine.evaluate(this, path).firstOrNull()

  fun doesSatisfyFilter(resource: Resource, filter: Filter): Boolean? {
    if (filter.valueCoding == null && filter.valueString == null)
      throw IllegalStateException("Filter must have either of one valueCoding or valueString")

    // get property mentioned as filter and match value
    // e.g. category: CodeableConcept in Condition
    return resource
      .getNamedProperty(filter.key)
      .values
      .firstOrNull()
      ?.let {
        when (it) {
          // match relevant type and value
          is CodeableConcept -> filter.valueCoding!!.isSimilar(it)
          is Coding -> filter.valueCoding!!.isSimilar(it)
          is StringType -> it.value == filter.valueString
          else -> false
        }
      }
      .also {
        if (it == null)
          Timber.i("${resource.resourceType}, ${filter.key}: could not resolve key value filter")
      }
  }

  suspend fun getCondition(encounter: Encounter, filter: Filter?) =
    getSearchResults<Condition>(
      encounter.referenceValue(),
      Condition.ENCOUNTER,
      filter,
      patientRepository.fhirEngine
    )
      .sortedByDescending {
        if (it.hasOnsetDateTimeType()) it.onsetDateTimeType.value else it.recordedDate
      }
      .sortedByDescending { it.logicalId }

  suspend fun getObservation(encounter: Encounter, filter: Filter?) =
    getSearchResults<Observation>(
      encounter.referenceValue(),
      Observation.ENCOUNTER,
      filter,
      patientRepository.fhirEngine
    )
      .sortedByDescending {
        if (it.hasEffectiveDateTimeType()) it.effectiveDateTimeType.value else it.meta.lastUpdated
      }
      .sortedByDescending { it.logicalId }

  suspend fun getMedicationRequest(encounter: Encounter, filter: Filter?) =
    getSearchResults<MedicationRequest>(
      encounter.referenceValue(),
      MedicationRequest.ENCOUNTER,
      filter,
      patientRepository.fhirEngine
    )
      .sortedByDescending { it.authoredOn }
      .sortedByDescending { it.logicalId }
}
