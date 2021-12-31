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

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.extension.isPatient
import org.smartregister.fhircore.quest.configuration.view.DetailViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.PatientRegisterRowViewConfiguration
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemCell
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemRow
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@HiltViewModel
class SimpleDetailsViewModel
@Inject
constructor(val patientRepository: PatientRepository): ViewModel(), SimpleDetailsDataProvider {

  override val detailsViewItem: MutableLiveData<DetailsViewItem>
    get() = MutableLiveData(null)

  val patientItem = MutableLiveData<PatientItem>()

  fun loadData(patientId: String) {
    viewModelScope.launch {
      val config =
        patientRepository.configurationRegistry.retrieveConfiguration<DetailViewConfiguration>(
          configClassification = QuestConfigClassification.DETAIL_VIEW
        )

      val dataItem = DetailsViewItem()
      dataItem.label = config.label

      config.rows.forEach {
        val row = DetailsViewItemRow()

        it.columns.forEach {
          // TODO patientRepository.getSearchResults(patientId, )
          // TODO row.cells.add(DetailsViewItemCell(, it))
        }

        dataItem.rows.add(row)
      }

      detailsViewItem.postValue(dataItem)
    }
  }

}
