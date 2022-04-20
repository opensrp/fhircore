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
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.asSearchFilter
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
    getRegisterDataFilters()!!.let { param ->
      // http://hl7.org/fhir/parameters.html -
      // Rule - inv-1: A parameter must have one and only one of (value, resource, part)
      if (arrayOf(param.hasValue(), param.hasResource(), param.hasPart()).count { it } != 1)
        throw UnsupportedOperationException(
          "${param.name} -> A parameter must have one and only one of (value, resource, part)"
        )
      // Rule - Only one level of nested parameters (part) is allowed
      // http://hl7.org/fhir/parameters-definitions.html#Parameters.parameter.part
      if (param.part.any { it.part.isNotEmpty() })
        throw UnsupportedOperationException(
          "${param.name} part field -> Only up to 1 level of nesting is supported"
        )

      return when {
        param.hasValue() ->
          loadValueData(param).map {
            RegisterData.RawRegisterData(
              id = it.logicalId,
              name = it.logicalId,
              healthModule = HealthModule.FAMILY,
              main = it,
              details = mapOf()
            )
          }
        param.hasResource() ->
          throw UnsupportedOperationException("Dont know resource handling") // TODO
        else -> {
          // for parts defined the config must define the mapping for main param
          val main =
            param.part.single { it.name == param.name }.let { mainParam ->
              loadValueData(mainParam)
            }

          main.map {
            // add main to context data
            val contextData = mutableMapOf<String, Any>(param.name to it)

            val details = mutableMapOf<String, List<Resource>>()
            param.part.filter { it.name != param.name }.forEach { partParam ->
              loadValueData(partParam, contextData).also { details[partParam.name] = it }.also {

                // add subsequent items to context data
                contextData[partParam.name] = it
              }
            }

            val mapper =
              configurationRegistry.retrieveDataMapperConfiguration(
                HealthModule.FAMILY.name
              )!! // TODO handle edges
            parseMapping(mapper, contextData, it)
              ?: RegisterData.RawRegisterData(
                id = it.logicalId,
                name = it.logicalId,
                healthModule = HealthModule.FAMILY,
                main = it,
                details = details
              )
          }
        }
      }
    }
  }

  fun parseMapping(
    mapperParam: Parameters.ParametersParameterComponent,
    contextData: MutableMap<String, Any>,
    base: Resource
  ): RegisterData {
    val className =
      mapperParam
        .extension
        .firstOrNull {
          it.url == "http://hl7.org/fhir/StructureDefinition/structuredefinition-explicit-type-name"
        }
        ?.value
        .toString()
    className.let {
      Class.forName(it).kotlin.constructors.first().apply {
        val paramValues = mutableMapOf<KParameter, Any?>()
        this.parameters.map { clsParam ->
          mapperParam
            .part
            .singleOrNull { it.name == clsParam.name }
            ?.let { partParam ->
              if (partParam.hasResource()) {
                if (clsParam.type.jvmErasure.isSubclassOf(Collection::class))
                  (contextData[partParam.name] as Collection<*>).map {
                    parseMapping(
                      (partParam.resource as Parameters).parameter.first(),
                      contextData,
                      it as Resource
                    ) // TODO handle first plus
                  }
                else
                  parseMapping(
                    (partParam.resource as Parameters).parameter.first(),
                    contextData,
                    contextData[partParam.name] as Resource
                  ) // TODO handle first plus
              } else
                partParam.value
                  ?.let {
                    fhirPathEngine
                      .evaluate(contextData, base, null, null, it.castToExpression(it).expression)
                      .firstOrNull()
                  }
                  ?.let { if (it.isPrimitive) (it as PrimitiveType<*>).value else it }
            }
            .also { evalValue -> paramValues[clsParam] = evalValue }
        }

        return this.callBy(paramValues) as RegisterData
      }
    }
  }

  suspend fun loadValueData(
    param: Parameters.ParametersParameterComponent,
    contextData: MutableMap<String, Any> = mutableMapOf()
  ): List<Resource> {
    with(param.castToDataRequirement(param.value!!)) {
      return defaultRepository.searchResourceFor(
        resourceType = ResourceType.fromCode(this.type),
        dataRequirement = this,
        contextData = contextData
      )
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, patientId: String): ProfileData {
    val family = fhirEngine.load(Patient::class.java, patientId)
    val members = loadFamilyMembersDetails(family.logicalId)
    val familyServices =
      defaultRepository.searchResourceFor<CarePlan>(
        subjectId = family.logicalId,
        subjectParam = CarePlan.SUBJECT // TODO do we need all CPs or Family CPs
      )

    return FamilyProfileMapper.transformInputToOutputModel(
      FamilyDetail(family, members, familyServices)
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine.count<Patient> {
      getRegisterDataFilters()
        ?.let { it.castToDataRequirement(it.value) }
        ?.asSearchFilter(fhirPathEngine)
        ?.forEach { filterBy(it) }
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
