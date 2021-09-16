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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.*
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asReference
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.*

class AncPatientRepository(
    override val fhirEngine: FhirEngine,
    override val domainMapper: DomainMapper<Patient, AncPatientItem>,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, AncPatientItem> {

    override suspend fun loadData(
        query: String,
        pageNumber: Int,
        loadAll: Boolean
    ): List<AncPatientItem> {
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

    override suspend fun countAll(): Long =
        withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

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
                            patientIdentifier = patient.logicalId,
                            name = patientHead.extractName(),
                            gender = patientHead.extractGender(AncApplication.getContext()) ?: "",
                            age = patientHead.extractAge(),
                            demographics = address
                        )
                } else {
                    ancPatientItemHead = AncPatientItem()
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
            fhirEngine.search { filter(CarePlan.PATIENT) { value = "Patient/$patientId" } }
        }

    suspend fun fetchObservations(patientId: String): List<Observation> =
        withContext(dispatcherProvider.io()) {
            fhirEngine.search { filter(Observation.PATIENT) { value = "Patient/$patientId" } }
        }

    suspend fun fetchEncounters(patientId: String): List<Encounter> =
        withContext(dispatcherProvider.io()) {
            fhirEngine.search { filter(Encounter.PATIENT) { value = "Patient/$patientId" } }
        }


    fun fetchCarePlanItem(carePlan: List<CarePlan>): List<CarePlanItem> {
        val listCarePlan = arrayListOf<CarePlanItem>()
        if (carePlan.isNotEmpty()) {
            for (i in 0..carePlan.size)
                listCarePlan.add(CarePlanItem(carePlan[i].title, carePlan[i].period.start))
        }
        return listCarePlan
    }

    suspend fun enrollIntoAnc(patient: Patient) {
        val pregnancyCondition = loadConfig(Template.PREGNANCY_CONDITION, Condition::class.java)
        pregnancyCondition.apply {
            this.id = getUniqueId()
            this.subject = patient.asReference()
            this.onset = DateTimeType.now()
        }
        fhirEngine.save(pregnancyCondition)

        val pregnancyEpisodeOfCase =
            loadConfig(Template.PREGNANCY_EPISODE_OF_CARE, EpisodeOfCare::class.java)
        pregnancyEpisodeOfCase.apply {
            this.id = getUniqueId()
            this.patient = patient.asReference()
            this.diagnosisFirstRep.condition = pregnancyCondition.asReference()
            this.period = Period().apply { this@apply.start = Date() }
            this.status = EpisodeOfCare.EpisodeOfCareStatus.ACTIVE
        }
        fhirEngine.save(pregnancyEpisodeOfCase)

        val pregnancyEncounter =
            loadConfig(Template.PREGNANCY_FIRST_ENCOUNTER, Encounter::class.java)
        pregnancyEncounter.apply {
            this.id = getUniqueId()
            this.status = Encounter.EncounterStatus.INPROGRESS
            this.subject = patient.asReference()
            this.episodeOfCare = listOf(pregnancyEpisodeOfCase.asReference())
            this.period = Period().apply { this@apply.start = Date() }
            this.diagnosisFirstRep.condition = pregnancyCondition.asReference()
        }
        fhirEngine.save(pregnancyEncounter)

        val pregnancyGoal =
            Goal().apply {
                this.id = getUniqueId()
                this.lifecycleStatus = Goal.GoalLifecycleStatus.ACTIVE
                this.subject = patient.asReference()
            }
        fhirEngine.save(pregnancyGoal)
    }

    private fun <T : Resource> loadConfig(id: String, clazz: Class<T>): T {
        return AncApplication.getContext().loadConfig(id, clazz)
    }

    suspend fun postVitalSigns(
        questionnaire: Questionnaire,
        questionnaireResponse: QuestionnaireResponse
    ) {
        val observation =
            ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Observation
        observation.id = getUniqueId()
//        fhirEngine.save(observation)

        // use ResourceMapper when supported by SDK
        val target = mutableListOf<Observation>()
        QuestionnaireUtils.extractObservations(
            questionnaireResponse,
            questionnaire.item,
            observation,
            target
        )
        target.forEach {
//            fhirEngine.save(it)
        }

    }


    companion object {
        object Template {
            const val PREGNANCY_CONDITION = "pregnancy_condition_template.json"
            const val PREGNANCY_EPISODE_OF_CARE = "pregnancy_episode_of_care_template.json"
            const val PREGNANCY_FIRST_ENCOUNTER = "pregnancy_first_encounter_template.json"
            const val ANC_VITAL_SIGNS = "vital_signs_questionnaire.json"
        }
    }
}
