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

package org.smartregister.fhircore.anc.ui.anccare.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName

class AncDetailsViewModel(
  var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
  val fhirEngine: FhirEngine,
  val patientId: String
) : ViewModel() {

  private var address: String = ""
  private lateinit var ancPatientItemHead: AncPatientItem
  val patientDemographics = MutableLiveData<AncPatientDetailItem>()
  val patientCarePlan = MutableLiveData<List<CarePlanItem>>()

  // Todo migrate to PatientRepository to follow repository pattern
  fun fetchDemographics() {
    if (patientId.isNotEmpty())
      viewModelScope.launch(dispatcher.io()) {
        val patient = fhirEngine.load(Patient::class.java, patientId)
        if (patient.link.isNotEmpty()) {
          val patientHead =
            fhirEngine.load(
              Patient::class.java,
              patient.link[0].other.reference.replace("Patient/", "")
            )
          if (patientHead.address.isNotEmpty()) {
            address = patientHead.address[0].country
          }
          ancPatientItemHead =
            AncPatientItem(
              patientHead.id,
              patientHead.extractName(),
              patientHead.extractGender(),
              patientHead.extractAge(),
              address
            )
        } else {
          ancPatientItemHead = AncPatientItem()
        }

        val ancPatientItem =
          AncPatientItem(
            patientId,
            patient.extractName(),
            patient.extractGender(),
            patient.extractAge()
          )
        val ancPatientDetailItem = AncPatientDetailItem(ancPatientItem, ancPatientItemHead)
        patientDemographics.postValue(ancPatientDetailItem)
      }
  }

  // Todo dynamically get carePlan once stored on FHIR and will be migrated to PatientRepository to
  // follow repository pattern
  fun fetchCarePlan(patientId: String, qJson: String?) {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    if (patientId.isNotEmpty())
      viewModelScope.launch(dispatcher.io()) {
        val listCarePlan = arrayListOf<CarePlanItem>()
        val carePlan = iParser.parseResource(qJson) as CarePlan
        listCarePlan.add(CarePlanItem(carePlan.title, carePlan.period.start))
        patientCarePlan.postValue(listCarePlan)
      }
  }
}
