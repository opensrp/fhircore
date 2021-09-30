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
import com.google.android.fhir.search.count
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.madx.model.PatientBMIItem
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.searchActivePatients

class PatientBmiRepository(
    override val fhirEngine: FhirEngine,
    override val domainMapper: DomainMapper<Patient, PatientBMIItem>,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, PatientBMIItem> {

    private val registerConfig =
        AncApplication.getContext().loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

    private val ancPatientRepository = AncPatientRepository(fhirEngine, AncItemMapper)

    private val resourceMapperExtended = ResourceMapperExtended(fhirEngine)

    override suspend fun loadData(
        query: String,
        pageNumber: Int,
        loadAll: Boolean
    ): List<PatientBMIItem> {
        return withContext(dispatcherProvider.io()) {
            val patients =
                fhirEngine.searchActivePatients(
                    query = query,
                    pageNumber = pageNumber,
                    loadAll = loadAll
                )
            patients.map { domainMapper.mapToDomainModel(it) }
        }
    }

    override suspend fun countAll(): Long {
        return fhirEngine.count<Patient> { filterBy(registerConfig.primaryFilter!!) }
    }

    suspend fun recordComputedBMI(
        questionnaire: Questionnaire,
        questionnaireResponse: QuestionnaireResponse,
        patientId: String,
        height: Double,
        weight: Double,
        computedBMI: Double
    ) {
        resourceMapperExtended.saveParsedResource(
            questionnaireResponse,
            questionnaire,
            patientId,
            null
        )
        ancPatientRepository.recordBmi(patientId, height, weight, computedBMI)
    }

}