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
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.madx.model.AllergiesItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.madx.model.AncPatientItem
import org.smartregister.fhircore.anc.data.madx.model.CarePlanItem
import org.smartregister.fhircore.anc.data.madx.model.ConditionItem
import org.smartregister.fhircore.anc.data.madx.model.EncounterItem
import org.smartregister.fhircore.anc.data.madx.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asReference
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.fhircore.engine.util.extension.overdue
import org.smartregister.fhircore.engine.util.extension.searchActivePatients

class NonAncPatientRepository(
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
        fhirEngine.searchActivePatients(query = query, pageNumber = pageNumber, loadAll = loadAll)
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
          if (patientHead.address != null)
            if (patientHead.address.isNotEmpty()) {
              if (patient.address[0].hasCountry()) address = patientHead.address[0].country
              else if (patient.address[0].hasCity()) address = patientHead.address[0].city
              else if (patient.address[0].hasState()) address = patientHead.address[0].state
              else if (patient.address[0].hasDistrict()) address = patientHead.address[0].district
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
      fhirEngine.search { filter(CarePlan.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchConditions(patientId: String): List<Condition> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Condition.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchEncounters(patientId: String): List<Encounter> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Encounter.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchPatient(patientId: String): Patient =
    withContext(dispatcherProvider.io()) { fhirEngine.load(Patient::class.java, patientId) }

  fun fetchCarePlanItem(carePlan: List<CarePlan>, patientId: String): List<CarePlanItem> {
    val listCarePlan = arrayListOf<CarePlanItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      listCarePlanList.addAll(carePlan.filter { it.overdue() })
      for (i in listCarePlanList.indices) {
        val title = if (listCarePlanList[i].title == null) "" else listCarePlanList[i].title
        listCarePlan.add(
          CarePlanItem(
            listCarePlanList[i].id,
            patientId,
            title,
            listCarePlanList[i].due(),
            listCarePlanList[i].overdue()
          )
        )
      }
    }
    return listCarePlan
  }

  suspend fun enrollIntoAnc(patient: Patient) {
    val pregnancyCondition = loadConfig(PREGNANCY_CONDITION, Condition::class.java)
    pregnancyCondition.apply {
      this.id = getUniqueId()
      this.subject = patient.asReference()
      this.onset = DateTimeType.now()
    }
    fhirEngine.save(pregnancyCondition)

    val pregnancyEpisodeOfCase = loadConfig(PREGNANCY_EPISODE_OF_CARE, EpisodeOfCare::class.java)
    pregnancyEpisodeOfCase.apply {
      this.id = getUniqueId()
      this.patient = patient.asReference()
      this.diagnosisFirstRep.condition = pregnancyCondition.asReference()
      this.period = Period().apply { this@apply.start = Date() }
      this.status = EpisodeOfCare.EpisodeOfCareStatus.ACTIVE
    }
    fhirEngine.save(pregnancyEpisodeOfCase)

    val pregnancyEncounter = loadConfig(PREGNANCY_FIRST_ENCOUNTER, Encounter::class.java)
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

  private fun <T : Resource> loadConfig(
    id: String,
    clazz: Class<T>,
    data: Map<String, String?> = emptyMap()
  ): T {
    return AncApplication.getContext().loadResourceTemplate(id, clazz, data)
  }

  suspend fun postVitalSigns(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    clientIdentifier: String?
  ) {}

  fun fetchEncounterItem(patientId: String, listEncounters: List<Encounter>): List<EncounterItem> {
    return arrayListOf()
  }

  fun fetchUpcomingServiceItem(
    patientId: String,
    carePlan: List<CarePlan>
  ): List<UpcomingServiceItem> {
    val listCarePlan = arrayListOf<UpcomingServiceItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      for (i in listCarePlanList.indices) {
        for (j in listCarePlanList[i].activity.indices) {
          var typeString = ""
          var dateString = ""
          if (listCarePlanList[i].activity[j].hasDetail())
            if (listCarePlanList[i].activity[j].detail.hasDescription())
              typeString = listCarePlanList[i].activity[j].detail.description
          if (listCarePlanList[i].activity[j].detail.hasScheduledPeriod())
            dateString =
              listCarePlanList[i].activity[j].detail.scheduledPeriod.start.makeItReadable()
          listCarePlan.add(
            UpcomingServiceItem(listCarePlanList[i].id, patientId, typeString, dateString)
          )
        }
      }
    }
    return listCarePlan
  }

  fun fetchConditionItem(patientId: String, listCondition: List<Condition>): List<ConditionItem> {
    return arrayListOf()
  }

  fun fetchAllergiesItem(patientId: String, listCondition: List<Condition>): List<AllergiesItem> {
    return arrayListOf()
  }

  companion object {
    const val PREGNANCY_CONDITION = "pregnancy_condition_template.json"
    const val PREGNANCY_EPISODE_OF_CARE = "pregnancy_episode_of_care_template.json"
    const val PREGNANCY_FIRST_ENCOUNTER = "pregnancy_first_encounter_template.json"
  }
}
