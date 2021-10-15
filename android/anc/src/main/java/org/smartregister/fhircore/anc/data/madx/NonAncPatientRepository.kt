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

package org.smartregister.fhircore.anc.data.madx

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.madx.model.AllergiesItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientItem
import org.smartregister.fhircore.anc.data.madx.model.CarePlanItem
import org.smartregister.fhircore.anc.data.madx.model.ConditionItem
import org.smartregister.fhircore.anc.data.madx.model.EncounterItem
import org.smartregister.fhircore.anc.data.madx.model.UpcomingServiceItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.overdue

class NonAncPatientRepository(
  val fhirEngine: FhirEngine,
  private val domainMapperCarePlan: DomainMapper<CarePlan, CarePlanItem>,
  private val domainMapperUpcomingService: DomainMapper<CarePlan, UpcomingServiceItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) {

  suspend fun fetchDemographics(patientId: String): AncPatientDetailItem {
    var ancPatientDetailItem = AncPatientDetailItem()
    if (patientId.isNotEmpty())
      withContext(dispatcherProvider.io()) {
        val patient = fhirEngine.load(Patient::class.java, patientId)
        var ancPatientItemHead = AncPatientItem()
        if (patient.link.isNotEmpty()) {
          var address = ""
          val patientHead =
            fhirEngine.load(
              Patient::class.java,
              patient.link[0].other.reference.replace("Patient/", "")
            )
          if (patientHead.address != null && patientHead.address.isNotEmpty()) {
            address =
              when {
                patient.address[0].hasCountry() -> patientHead.address[0].country
                patient.address[0].hasCity() -> patientHead.address[0].city
                patient.address[0].hasState() -> patientHead.address[0].state
                patient.address[0].hasDistrict() -> patientHead.address[0].district
                else -> ""
              }
          }
          ancPatientItemHead =
            AncPatientItem(
              patientIdentifier = patient.logicalId,
              name = patientHead.extractName(),
              gender = patientHead.extractGender(AncApplication.getContext()) ?: "",
              age = patientHead.extractAge(),
              demographics = address
            )
        }

        val ancPatientItem =
          AncPatientItem(
            patientIdentifier = patient.logicalId,
            name = patient.extractName(),
            gender = patient.extractGender(AncApplication.getContext()) ?: "",
            age = patient.extractAge()
          )
        ancPatientDetailItem = AncPatientDetailItem(ancPatientItem, ancPatientItemHead)
      }
    return ancPatientDetailItem
  }

  suspend fun fetchCarePlan(patientId: String): List<CarePlan> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(CarePlan.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchEncounters(patientId: String): List<Encounter> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Encounter.SUBJECT) { value = "Patient/$patientId" } }
    }

  fun fetchCarePlanItem(carePlan: List<CarePlan>, patientId: String): List<CarePlanItem> {
    val listCarePlan = arrayListOf<CarePlanItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      listCarePlanList.addAll(carePlan.filter { it.overdue() })
      for (i in listCarePlanList.indices) {
        listCarePlan.add(domainMapperCarePlan.mapToDomainModel(listCarePlanList[i]))
      }
    }
    return listCarePlan
  }

  fun fetchUpcomingServiceItem(carePlan: List<CarePlan>): List<UpcomingServiceItem> {
    val listCarePlan = arrayListOf<UpcomingServiceItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      for (i in listCarePlanList.indices) {
        listCarePlan.add(domainMapperUpcomingService.mapToDomainModel(listCarePlanList[i]))
      }
    }
    return listCarePlan
  }

  fun fetchConditionItem(listCondition: List<Condition>): List<ConditionItem> {
    return arrayListOf()
  }

  fun fetchAllergiesItem(listCondition: List<Condition>): List<AllergiesItem> {
    return arrayListOf()
  }

  fun fetchEncounterItem(listEncounters: List<Encounter>): List<EncounterItem> {
    return arrayListOf()
  }
}
