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

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.count
import com.google.android.fhir.search.getQuery
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.UnitConstants
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.ui.anccare.details.CarePlanItemMapper
import org.smartregister.fhircore.anc.ui.anccare.details.EncounterItemMapper
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.anc.ui.anccare.shared.AncItemMapper
import org.smartregister.fhircore.anc.util.AncOverviewType
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.SearchFilter
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.anc.util.loadRegisterConfigAnc
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.format
import org.smartregister.fhircore.engine.util.extension.generateUniqueId
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.overdue

enum class DeletionReason(@StringRes val label: Int) {
  ENTRY_IN_ERROR(R.string.remove_this_person_reason_error_entry),
  MOVED_OUT(R.string.remove_this_person_reason_moved_out)
}

class PatientRepository
@Inject
constructor(
  @ApplicationContext val context: Context,
  override val fhirEngine: FhirEngine,
  override val domainMapper: AncItemMapper,
  val dispatcherProvider: DispatcherProvider
) : RegisterRepository<Anc, PatientItem> {

  private val registerConfig = context.loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

  private val ancOverviewConfig = context.loadRegisterConfigAnc(AncOverviewType.ANC_OVERVIEW_ID)
  private val vitalSignsConfig = context.loadRegisterConfigAnc(AncOverviewType.VITAL_SIGNS)

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val pregnancies =
        fhirEngine
          .search<Condition> {
            filterBy(registerConfig.primaryFilter!!)
            registerConfig.secondaryFilter?.let { filterBy(it) }

            count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
            from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
          }
          .distinctBy { it.subject.extractId() }

      val patients =
        pregnancies
          .map { fhirEngine.get<Patient>(it.subject.extractId()) }
          .filter { it.active }
          .sortedBy { it.nameFirstRep.family }

      patients.map {
        val head =
          kotlin
            .runCatching { fhirEngine.get<Patient>(it.link[0].id.replace("Patient/", "")) }
            .getOrNull()

        val carePlans = searchCarePlan(it.logicalId)
        val conditions = searchCondition(it.logicalId)
        domainMapper.mapToDomainModel(Anc(it, head, conditions, carePlans))
      }
    }
  }

  suspend fun searchPatientByLink(linkId: String): List<Patient> {
    return fhirEngine.search {
      filterByResourceTypeId(Patient.LINK, ResourceType.Patient, linkId)
      filter(Patient.ACTIVE, { value = of(true) })
    }
  }

  suspend fun searchCondition(patientId: String): List<Condition> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, patientId)
      }
    }

  suspend fun searchCarePlan(patientId: String, tag: Coding? = null): List<CarePlan> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, patientId)

        tag?.run {
          filterBy(
            SearchFilter(
              "_tag",
              Enumerations.SearchParamType.TOKEN,
              Enumerations.DataType.CODING,
              tag
            )
          )
        }
      }
    }

  suspend fun revokeCarePlans(patientId: String, tag: Coding? = null) {
    // revoke all incomplete careplans
    searchCarePlan(patientId, tag).forEach {
      if (it.status != CarePlan.CarePlanStatus.COMPLETED) {
        it.status = CarePlan.CarePlanStatus.REVOKED
        it.period.end = Date()

        fhirEngine.create(it)
      }
    }
  }

  suspend fun revokeActiveStatusData(patientId: String) {
    revokeFlags(patientId)
    revokeConditions(patientId)
  }

  suspend fun revokeFlags(patientId: String) {
    fhirEngine
      .search<Flag> { filterByResourceTypeId(Flag.PATIENT, ResourceType.Patient, patientId) }
      .filter { it.status == Flag.FlagStatus.ACTIVE }
      .forEach {
        it.status = Flag.FlagStatus.INACTIVE
        it.period.end = Date()

        fhirEngine.create(it)
      }
  }

  suspend fun revokeConditions(patientId: String) {
    fhirEngine
      .search<Condition> {
        filterByResourceTypeId(Condition.PATIENT, ResourceType.Patient, patientId)
      }
      .filter { it.clinicalStatus.codingFirstRep.code == "active" }
      .forEach {
        it.clinicalStatus.codingFirstRep.code = "inactive"
        it.abatement = DateTimeType(Date())

        fhirEngine.create(it)
      }
  }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) {
      fhirEngine.count<Condition> {
        filterBy(registerConfig.primaryFilter!!)
        registerConfig.secondaryFilter?.let { filterBy(it) }
      }
    }

  suspend fun fetchDemographics(patientId: String): PatientDetailItem {
    var ancPatientDetailItem = PatientDetailItem()
    if (patientId.isNotEmpty())
      withContext(dispatcherProvider.io()) {
        val patient = fhirEngine.get<Patient>(patientId)
        var ancPatientItemHead = PatientItem()
        if (patient.link.isNotEmpty()) {
          val patientHead =
            fhirEngine.get<Patient>(patient.linkFirstRep.other.reference.replace("Patient/", ""))

          ancPatientItemHead =
            PatientItem(
              patientIdentifier = patient.logicalId,
              name = patientHead.extractName(),
              gender = patientHead.extractGender(context) ?: "",
              birthDate = patientHead.birthDate,
              address = patientHead.extractAddress()
            )
        }

        val ancPatientItem =
          PatientItem(
            patientIdentifier = patient.logicalId,
            name = patient.extractName(),
            gender = patient.extractGender(context) ?: "",
            isPregnant = searchCondition(patient.logicalId).hasActivePregnancy(),
            birthDate = patient.birthDate,
            address = patient.extractAddress(),
            isHouseHoldHead = patient.link.isEmpty(),
            headId = ancPatientItemHead.patientIdentifier
          )
        ancPatientDetailItem = PatientDetailItem(ancPatientItem, ancPatientItemHead)
      }
    return ancPatientDetailItem
  }

  suspend fun fetchActiveFlag(patientId: String, flagCode: Coding): Flag? {
    return fhirEngine
      .search<Flag> { filterByResourceTypeId(Flag.PATIENT, ResourceType.Patient, patientId) }
      .firstOrNull {
        it.status == Flag.FlagStatus.ACTIVE &&
          it.code.coding.any { coding -> coding.code == flagCode.code }
      }
  }

  fun fetchCarePlanItem(carePlan: List<CarePlan>): List<CarePlanItem> =
    carePlan.filter { it.due() || it.overdue() }.map { CarePlanItemMapper.mapToDomainModel(it) }

  suspend fun fetchCarePlan(patientId: String): List<CarePlan> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(CarePlan.SUBJECT, { value = "Patient/$patientId" }) }
    }

  suspend fun fetchObservations(patientId: String, searchFilterString: String): Observation {
    val searchFilter: SearchFilter =
      when (searchFilterString) {
        "edd" -> ancOverviewConfig.eddFilter!!
        "risk" -> ancOverviewConfig.riskFilter!!
        "fetuses" -> ancOverviewConfig.fetusesFilter!!
        "ga" -> ancOverviewConfig.gaFilter!!
        else ->
          throw UnsupportedOperationException("Given filter $searchFilterString not supported")
      }
    var finalObservation = Observation()
    val observations =
      withContext(dispatcherProvider.io()) {
        fhirEngine.search<Observation> {
          filterBy(searchFilter)
          // for patient filter use extension created
          filterByResourceTypeId(Observation.SUBJECT, ResourceType.Patient, patientId)
        }
      }
    if (observations.isNotEmpty())
      finalObservation = observations.sortedBy { it.effectiveDateTimeType.value }.first()

    return finalObservation
  }

  suspend fun fetchVitalSigns(patientId: String, searchFilterString: String): Observation {
    var searchFilter: SearchFilter
    with(vitalSignsConfig) {
      searchFilter =
        when (searchFilterString) {
          "body-weight" -> weightFilter!!
          "body-height" -> heightFilter!!
          "bmi" -> bmiFilter!!
          "bp-s" -> bpsFilter!!
          "bp-d" -> bpdsFilter!!
          "pulse-rate" -> pulseRateFilter!!
          "bg" -> bloodGlucoseFilter!!
          "spO2" -> bloodOxygenLevelFilter!!
          else ->
            throw UnsupportedOperationException("Given filter $searchFilterString not supported")
        }
    }
    val observations =
      withContext(dispatcherProvider.io()) {
        fhirEngine.search<Observation> {
          filterBy(searchFilter)
          // for patient filter use extension created
          filterByResourceTypeId(Observation.SUBJECT, ResourceType.Patient, patientId)
        }
      }

    return observations.maxByOrNull { it.effectiveDateTimeType.value } ?: Observation()
  }

  suspend fun fetchEncounters(patientId: String): List<Encounter> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Encounter.SUBJECT, { value = "Patient/$patientId" }) }
    }

  suspend fun markDeceased(patientId: String, deathDate: Date) {
    withContext(dispatcherProvider.io()) {
      val patient = fhirEngine.get<Patient>(patientId)

      if (!patient.active) throw IllegalStateException("Patient already deleted")

      if (patient.hasDeceased()) throw IllegalStateException("Patient already marked deceased")

      patient.deceased = DateTimeType(deathDate)

      revokeCarePlans(patientId)
      revokeActiveStatusData(patientId)

      fhirEngine.create(patient)
    }
  }

  suspend fun deletePatient(patientId: String, reason: DeletionReason) {
    withContext(dispatcherProvider.io()) {
      val patient = fhirEngine.get<Patient>(patientId)

      if (!patient.active) throw IllegalStateException("Patient already deleted")

      if (patient.isFamilyHead())
        throw IllegalStateException("A patient representing family can not be deleted")

      when (reason) {
        DeletionReason.MOVED_OUT -> {
          patient.link.clear()
        }
        DeletionReason.ENTRY_IN_ERROR -> {
          patient.active = false
          patient.link.clear()
          revokeCarePlans(patientId)
          revokeActiveStatusData(patientId)
        }
      }

      fhirEngine.create(patient)
    }
  }

  private fun <T : Resource> loadConfig(
    id: String,
    clazz: Class<T>,
    data: Map<String, String?> = emptyMap()
  ): T {
    return context.loadResourceTemplate(id, clazz, data)
  }

  suspend fun fetchUpcomingServiceItem(carePlan: List<CarePlan>): List<UpcomingServiceItem> {
    val listCarePlan = arrayListOf<UpcomingServiceItem>()
    val listCarePlanList = arrayListOf<CarePlan>()
    if (carePlan.isNotEmpty()) {
      listCarePlanList.addAll(carePlan.filter { it.due() })
      listCarePlanList.forEach {
        var task: Task
        withContext(dispatcherProvider.io()) {
          val carePlanId = it.logicalId
          val tasks =
            fhirEngine.search<Task> {
              apply { filter(Task.FOCUS, { value = "CarePlan/$carePlanId" }) }.getQuery()
            }
          if (!tasks.isNullOrEmpty()) {
            task = tasks[0]
            listCarePlan.add(
              UpcomingServiceItem(
                task.logicalId,
                task.code.text,
                task.executionPeriod?.start.makeItReadable()
              )
            )
          }
        }
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

  suspend fun recordComputedBmi(
    patientId: String,
    encounterId: String,
    weight: Double,
    height: Double,
    computedBmi: Double,
    isUnitModeMetric: Boolean
  ): Boolean {
    return recordBmi(
      patientId = patientId,
      formEncounterId = encounterId,
      weight = weight,
      height = height,
      computedBmi = computedBmi,
      isUnitModeMetric = isUnitModeMetric
    )
  }

  suspend fun recordBmi(
    patientId: String,
    formEncounterId: String,
    weight: Double? = null,
    height: Double? = null,
    computedBmi: Double? = null,
    isUnitModeMetric: Boolean
  ): Boolean {
    var weightUnit = UnitConstants.UNIT_WEIGHT_USC
    var heightUnit = UnitConstants.UNIT_HEIGHT_USC
    var weightUnitCode = UnitConstants.UNIT_CODE_WEIGHT_USC
    var heightUnitCode = UnitConstants.UNIT_CODE_HEIGHT_USC
    var bmiUnit = UnitConstants.UNIT_BMI_USC
    if (isUnitModeMetric) {
      weightUnit = UnitConstants.UNIT_WEIGHT_METRIC
      heightUnit = UnitConstants.UNIT_HEIGHT_METRIC
      weightUnitCode = UnitConstants.UNIT_CODE_WEIGHT_METRIC
      heightUnitCode = UnitConstants.UNIT_CODE_HEIGHT_METRIC
      bmiUnit = UnitConstants.UNIT_BMI_METRIC
    }

    val bmiEncounterData = buildBmiConfigData(patientId = patientId, recordId = formEncounterId)
    val bmiEncounter = loadConfig(Template.BMI_ENCOUNTER, Encounter::class.java, bmiEncounterData)
    fhirEngine.create(bmiEncounter)

    val bmiWeightObservationRecordId = ResourceType.Observation.generateUniqueId()
    val bmiWeightObservationData =
      buildBmiConfigData(
        patientId = patientId,
        recordId = bmiWeightObservationRecordId,
        bmiEncounter = bmiEncounter,
        weight = weight,
        weightUnit = weightUnit,
        weightUnitCode = weightUnitCode
      )
    val bmiWeightObservation =
      loadConfig(Template.BMI_PATIENT_WEIGHT, Observation::class.java, bmiWeightObservationData)
    fhirEngine.create(bmiWeightObservation)

    val bmiHeightObservationRecordId = ResourceType.Observation.generateUniqueId()
    val bmiHeightObservationData =
      buildBmiConfigData(
        patientId = patientId,
        recordId = bmiHeightObservationRecordId,
        bmiEncounter = bmiEncounter,
        height = height,
        heightUnit = heightUnit,
        heightUnitCode = heightUnitCode
      )
    val bmiHeightObservation =
      loadConfig(Template.BMI_PATIENT_HEIGHT, Observation::class.java, bmiHeightObservationData)
    fhirEngine.create(bmiHeightObservation)

    val bmiObservationRecordId = ResourceType.Observation.generateUniqueId()
    val bmiObservationData =
      buildBmiConfigData(
        patientId = patientId,
        recordId = bmiObservationRecordId,
        bmiEncounter = bmiEncounter,
        computedBmi = computedBmi,
        bmiUnit = bmiUnit,
        refObsWeightFormId = bmiWeightObservationRecordId,
        refObsHeightFormId = bmiHeightObservationRecordId
      )
    val bmiObservation =
      loadConfig(Template.BMI_PATIENT_BMI, Observation::class.java, bmiObservationData)
    fhirEngine.create(bmiObservation)
    return bmiObservationData.isNotEmpty()
  }

  private fun buildBmiConfigData(
    recordId: String,
    patientId: String,
    bmiEncounter: Encounter? = null,
    height: Double? = null,
    weight: Double? = null,
    computedBmi: Double? = null,
    refObsWeightFormId: String? = null,
    refObsHeightFormId: String? = null,
    heightUnit: String? = null,
    weightUnit: String? = null,
    bmiUnit: String? = null,
    heightUnitCode: String? = null,
    weightUnitCode: String? = null,
  ): Map<String, String?> {
    return mapOf(
      "#Id" to recordId,
      "#RefPatient" to "Patient/$patientId",
      "#RefEncounter" to bmiEncounter?.id,
      "#RefPractitioner" to "Practitioner/399",
      "#RefDateStart" to DateType(Date()).format(),
      "#EffectiveDateTime" to DateTimeType(Date()).format(),
      "#WeightValue" to weight?.toString(),
      "#HeightValue" to height?.toString(),
      "#BmiValue" to computedBmi.toString(),
      "#RefIdObservationBodyHeight" to refObsHeightFormId,
      "#RefIdObservationBodyWeight" to refObsWeightFormId,
      "#WeightUnit" to weightUnit?.toString(),
      "#HeightUnit" to heightUnit?.toString(),
      "#BmiUnit" to bmiUnit?.toString(),
      "#CodeWeightUnit" to weightUnitCode?.toString(),
      "#CodeHeightUnit" to heightUnitCode?.toString(),
    )
  }

  fun setAncItemMapperType(ancItemMapperType: AncItemMapper.AncItemMapperType) {
    domainMapper.setAncItemMapperType(ancItemMapperType)
  }

  companion object {
    object Template {
      const val BMI_ENCOUNTER = "bmi_patient_encounter_template.json"
      const val BMI_PATIENT_WEIGHT = "bmi_patient_weight_observation_template.json"
      const val BMI_PATIENT_HEIGHT = "bmi_patient_height_observation_template.json"
      const val BMI_PATIENT_BMI = "bmi_patient_computed_bmi_observation_template.json"
    }
  }
}
