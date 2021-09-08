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

package org.smartregister.fhircore.anc.data

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.searchPatients

class AncPatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Patient, AncPatientItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, AncPatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
  ): List<AncPatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients = fhirEngine.searchPatients(query, pageNumber)
      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }

  suspend fun fetchDemographics(patientId: String): AncPatientDetailItem {
    var ancPatientDetailItem = AncPatientDetailItem()
    if (patientId.isNotEmpty())
      withContext(dispatcherProvider.io()) {
        val patient = fhirEngine.load(Patient::class.java, patientId)
        lateinit var ancPatientItemHead: AncPatientItem
        if (patient.link.isNotEmpty()) {
          var address = ""
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
              patient.logicalId,
              patientHead.extractName(),
              patientHead.extractGender(AncApplication.getContext()),
              patientHead.extractAge(),
              address
            )
        } else {
          ancPatientItemHead = AncPatientItem()
        }

        val ancPatientItem =
          AncPatientItem(
            patient.logicalId,
            patient.extractName(),
            patient.extractGender(AncApplication.getContext()),
            patient.extractAge()
          )
        ancPatientDetailItem = AncPatientDetailItem(ancPatientItem, ancPatientItemHead)
      }
    return ancPatientDetailItem
  }

  suspend fun fetchCarePlan(patientId: String, qJson: String?): List<CarePlanItem> {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val listCarePlan = arrayListOf<CarePlanItem>()
    if (patientId.isNotEmpty())
      withContext(dispatcherProvider.io()) {
        val carePlan = iParser.parseResource(qJson) as CarePlan
        listCarePlan.add(CarePlanItem(carePlan.title, carePlan.period.start))
      }
    return listCarePlan
  }
}
