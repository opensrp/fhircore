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

package org.smartregister.fhircore.anc.data.patient

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Goal
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asPatientReference
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asReference
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.anc.ui.anccare.details.CarePlanItemMapper
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
import org.smartregister.fhircore.anc.ui.anccare.register.Anc
import org.smartregister.fhircore.anc.util.AncOverviewType
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.SearchFilter
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.filterByPatient
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.anc.util.loadRegisterConfigAnc
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.format
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.fhircore.engine.util.extension.overdue
import org.smartregister.fhircore.engine.util.extension.plusMonthsAsString
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

class PatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Anc, AncPatientItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Anc, AncPatientItem> {

  private val registerConfig =
    AncApplication.getContext().loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

  private val ancOverviewConfig =
    AncApplication.getContext().loadRegisterConfigAnc(AncOverviewType.ANC_OVERVIEW_ID)

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<AncPatientItem> {
    return withContext(dispatcherProvider.io()) {
      val pregnancies =
        fhirEngine.search<Condition> {
          filterBy(registerConfig.primaryFilter!!)
          registerConfig.secondaryFilter?.let { filterBy(it) }

          count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      val patients =
        pregnancies.map { fhirEngine.load(Patient::class.java, it.subject.extractId()) }.sortedBy {
          it.nameFirstRep.family
        }

      patients.map {
        val head =
          kotlin
            .runCatching {
              fhirEngine.load(Patient::class.java, it.link[0].id.replace("Patient/", ""))
            }
            .getOrNull()

        val carePlans = searchCarePlan(it.logicalId)
        domainMapper.mapToDomainModel(Anc(it, head, carePlans))
      }
    }
  }

  suspend fun searchCarePlan(id: String): List<CarePlan> {
    return fhirEngine.search { filterByPatient(CarePlan.SUBJECT, id) }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) {
      fhirEngine.count<Condition> {
        filterBy(registerConfig.primaryFilter!!)
        registerConfig.secondaryFilter?.let { filterBy(it) }
      }
    }

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

  fun fetchCarePlanItem(carePlan: List<CarePlan>): List<CarePlanItem> {
    val listCarePlan = arrayListOf<CarePlanItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      listCarePlanList.addAll(carePlan.filter { it.overdue() })
      for (i in listCarePlanList.indices) {
        listCarePlan.add(CarePlanItemMapper.mapToDomainModel(listCarePlanList[i]))
      }
    }
    return listCarePlan
  }

  suspend fun fetchCarePlan(patientId: String): List<CarePlan> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(CarePlan.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun fetchObservations(patientId: String, searchFilterString: String): Observation {
    val searchFilter: SearchFilter =
      when (searchFilterString) {
        "edd" -> ancOverviewConfig.eddFilter!!
        "risk" -> ancOverviewConfig.riskFilter!!
        "fetueses" -> ancOverviewConfig.fetusesFilter!!
        "ga" -> ancOverviewConfig.gaFilter!!
        else -> ancOverviewConfig.eddFilter!!
      }
    var finalObservation = Observation()
    val observations =
      withContext(dispatcherProvider.io()) {
        fhirEngine.search<Observation> {
          filterBy(searchFilter)
          // for patient filter use extension created
          filterByPatient(Observation.SUBJECT, patientId)
        }
      }
    if (observations.isNotEmpty())
      finalObservation = observations.sortedBy { it.effectiveDateTimeType.value }.first()

    return finalObservation
  }

  suspend fun fetchEncounters(patientId: String): List<Encounter> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Encounter.SUBJECT) { value = "Patient/$patientId" } }
    }

  suspend fun enrollIntoAnc(patientId: String, lmp: DateType) {
    val conditionData = buildConfigData(patientId = patientId, lmp = lmp)

    val pregnancyCondition =
      loadConfig(Template.PREGNANCY_CONDITION, Condition::class.java, conditionData)
    fhirEngine.save(pregnancyCondition)

    val episodeData =
      buildConfigData(patientId = patientId, pregnancyCondition = pregnancyCondition, lmp = lmp)
    val pregnancyEpisodeOfCase =
      loadConfig(Template.PREGNANCY_EPISODE_OF_CARE, EpisodeOfCare::class.java, episodeData)
    fhirEngine.save(pregnancyEpisodeOfCase)

    val encounterData =
      buildConfigData(
        patientId = patientId,
        pregnancyCondition = pregnancyCondition,
        pregnancyEpisodeOfCase = pregnancyEpisodeOfCase,
        lmp = lmp
      )
    val pregnancyEncounter =
      loadConfig(Template.PREGNANCY_FIRST_ENCOUNTER, Encounter::class.java, encounterData)
    fhirEngine.save(pregnancyEncounter)

    val goalData = buildConfigData(patientId)
    val pregnancyGoal = loadConfig(Template.PREGNANCY_GOAL, Goal::class.java, goalData)
    fhirEngine.save(pregnancyGoal)

    val careplanData =
      buildConfigData(
        patientId = patientId,
        pregnancyCondition = pregnancyCondition,
        pregnancyEpisodeOfCase = pregnancyEpisodeOfCase,
        pregnancyEncounter = pregnancyEncounter,
        pregnancyGoal = pregnancyGoal,
        lmp = lmp
      )
    val pregnancyCarePlan =
      loadConfig(Template.PREGNANCY_CARE_PLAN, CarePlan::class.java, careplanData)
    fhirEngine.save(pregnancyCarePlan)
  }

  private fun <T : Resource> loadConfig(
    id: String,
    clazz: Class<T>,
    data: Map<String, String?> = emptyMap()
  ): T {
    return AncApplication.getContext().loadResourceTemplate(id, clazz, data)
  }

  private fun buildConfigData(
    patientId: String,
    pregnancyCondition: Condition? = null,
    pregnancyEpisodeOfCase: EpisodeOfCare? = null,
    pregnancyEncounter: Encounter? = null,
    pregnancyGoal: Goal? = null,
    lmp: DateType? = null
  ): Map<String, String?> {
    return mapOf(
      "#Id" to getUniqueId(),
      "#RefPatient" to asPatientReference(patientId).reference,
      "#RefCondition" to pregnancyCondition?.id,
      "#RefEpisodeOfCare" to pregnancyEpisodeOfCase?.id,
      "#RefEncounter" to pregnancyEncounter?.id,
      "#RefGoal" to pregnancyGoal?.asReference()?.reference,
      // TODO https://github.com/opensrp/fhircore/issues/560
      // add careteam and practitioner ref when available into all entities below where required
      "#RefCareTeam" to "CareTeam/325",
      "#RefPractitioner" to "Practitioner/399",
      "#RefDateOnset" to lmp?.format(),
      "#RefDateStart" to lmp?.format(),
      "#RefDateEnd" to lmp?.plusMonthsAsString(9),
      "#RefDate20w" to lmp?.plusWeeksAsString(20),
      "#RefDate26w" to lmp?.plusWeeksAsString(26),
      "#RefDate30w" to lmp?.plusWeeksAsString(30),
      "#RefDate34w" to lmp?.plusWeeksAsString(34),
      "#RefDate36w" to lmp?.plusWeeksAsString(36),
      "#RefDate38w" to lmp?.plusWeeksAsString(38),
      "#RefDate40w" to lmp?.plusWeeksAsString(40),
      "#RefDateDeliveryStart" to lmp?.plusWeeksAsString(40),
      "#RefDateDeliveryEnd" to lmp?.plusWeeksAsString(42),
    )
  }

  fun fetchUpcomingServiceItem(carePlan: List<CarePlan>): List<UpcomingServiceItem> {
    val listCarePlan = arrayListOf<UpcomingServiceItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      for (i in listCarePlanList.indices) {
        listCarePlan.add(CarePlanItemMapper.mapToUpcomingServiceItem(listCarePlanList[i]))
      }
    }
    return listCarePlan
  }

  fun fetchLastSeenItem(encounters: List<Encounter>): List<EncounterItem> {
    val listCarePlan = arrayListOf<EncounterItem>()
    if (encounters.isNotEmpty()) {
      for (i in encounters.indices) {
        listCarePlan.add(EncounterItemMapper.mapToDomainModel(encounters[i]))
      }
    }
    return listCarePlan
  }

  companion object {
    object Template {
      const val PREGNANCY_CONDITION = "pregnancy_condition_template.json"
      const val PREGNANCY_EPISODE_OF_CARE = "pregnancy_episode_of_care_template.json"
      const val PREGNANCY_FIRST_ENCOUNTER = "pregnancy_first_encounter_template.json"
      const val PREGNANCY_GOAL = "pregnancy_goal_template.json"
      const val PREGNANCY_CARE_PLAN = "pregnancy_careplan_template.json"
    }
  }
}
