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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.toCoding
import timber.log.Timber

@Singleton
class FamilyRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val fhirPathEngine: FHIRPathEngine
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val dataMap = mutableMapOf<String, Any>()
    getRegisterDataFilters()?.let { param ->
      // main data param for the register. this is the filter that defines the data like family,
      // anc, pnc, child etc
      val main = // todo ???
        with(param.castToDataRequirement(param.value)) {
          defaultRepository.searchResource(
            resourceType = ResourceType.fromCode(this.type),
            dataRequirement = this,
            currentPage = currentPage,
            limit = limit(loadAll, appFeatureName)
          )
        }

      if (param.part.any { it.part.isNotEmpty() })
        throw UnsupportedOperationException(
          "${param.name} part field -> Only up to 1 level of nesting is supported"
        )

      val parts = mutableMapOf<String, List<Resource>>()

      // the parts defines further details for the each item in data. i.e careplans, tasks,
      // encounters etc
      param.part.forEach { partParam ->
        partParam.name // todo ???

        with(partParam.castToDataRequirement(partParam.value)) {
          main.map { d ->
            defaultRepository.searchResource(
                resourceType = ResourceType.fromCode(this.type),
                dataRequirement = this,
                contextData = mapOf(param.name to d)
              )
              .apply { parts[partParam.name] = this }
          }
        }
      }
    }

    return patients.map { p ->
      val members = loadFamilyMembers(p.logicalId)
      val familyServices =
        defaultRepository.searchResourceFor<CarePlan>(
          subjectId = p.logicalId,
          subjectParam = CarePlan.SUBJECT,
          filters = getRegisterDataFilters()
        )

      FamilyMapper.transformInputToOutputModel(Family(p, members, familyServices))
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, patientId: String): ProfileData {
    val family = fhirEngine.load(Patient::class.java, patientId)
    val members = loadFamilyMembersDetails(family.logicalId)
    val familyServices =
      defaultRepository.searchResourceFor<CarePlan>(
        subjectId = family.logicalId,
        subjectParam = CarePlan.SUBJECT,
        filters = getRegisterDataFilters()
      )

    return FamilyProfileMapper.transformInputToOutputModel(
      FamilyDetail(family, members, familyServices)
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine.count<Patient> {
      getRegisterDataFilters().forEach { filterBy(it) }
      filter(Patient.ACTIVE, { value = of(true) })
    }
  }

  suspend fun limit(loadAll: Boolean, appFeatureName: String?): Int {
    return if (loadAll) countRegisterData(appFeatureName).toInt()
    else PaginationConstant.DEFAULT_PAGE_SIZE
  }

  suspend fun loadFamilyMembers(familyId: String) =
    fhirEngine
      .search<Patient> { filter(Patient.LINK, { value = familyId }) }
      // also include head
      .plus(fhirEngine.load(Patient::class.java, familyId))
      .map {
        val conditions =
          fhirEngine.search<Condition> {
            filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, it.logicalId)
          }
        val careplans =
          fhirEngine.search<CarePlan> {
            filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, it.logicalId)
            filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCoding()) })
          }
        FamilyMember(it, conditions, careplans)
      }

  suspend fun loadFamilyMembersDetails(familyId: String) =
    loadFamilyMembers(familyId).map {
      val flags =
        fhirEngine.search<Flag> {
          filterByResourceTypeId(Flag.SUBJECT, ResourceType.Patient, it.patient.id)
        }

      FamilyMemberDetail(it.patient, it.conditions, flags, it.servicesDue)
    }

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.FAMILY.name).also {
      if (it == null) {
        with(
          configurationRegistry.context.getString(R.string.health_module_filters_not_configured)
        ) {
          Timber.e(this)
          configurationRegistry.context.showToast(this)
        }
      }
    }
}
