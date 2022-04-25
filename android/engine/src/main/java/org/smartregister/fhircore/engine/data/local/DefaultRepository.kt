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

package org.smartregister.fhircore.engine.data.local

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.getLocalizedText
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.configuration.view.asCode
import org.smartregister.fhircore.engine.configuration.view.asSearchFilter
import org.smartregister.fhircore.engine.configuration.view.expressionExtension
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.ui.components.register.DEFAULT_MAX_PAGE_COUNT
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadPatientImmunizations
import org.smartregister.fhircore.engine.util.extension.loadRelatedPersons
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.updateFrom

@Singleton
open class DefaultRepository
@Inject
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val fhirPathEngine: FHIRPathEngine,
) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(resourceId) }
  }

  suspend fun loadRelatedPersons(patientId: String): List<RelatedPerson>? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadRelatedPersons(patientId) }
  }

  suspend fun loadPatientImmunizations(patientId: String): List<Immunization>? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadPatientImmunizations(patientId) }
  }

  suspend fun loadImmunization(immunizationId: String): Immunization? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(immunizationId) }
  }

  suspend fun loadQuestionnaireResponses(patientId: String, questionnaire: Questionnaire) =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = "Patient/$patientId" })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/${questionnaire.id}" })
      }
    }

  // TODO handle date-filter, value-set, multi-value code-filter
  fun DataRequirement.asSearchFilter(focusResource: Resource? = null) =
    codeFilter.map {
      // by definition path or searchParam are mutually exclusive
      SearchFilter(key = it.path ?: it.searchParam).apply {
        if (!it.hasCode() && it.extension.expressionExtension() == null)
          throw UnsupportedOperationException(
            "Either code or value expression for extension cqf-initiatingPerson should be specified"
          )

        if (it.hasCode()) valueCoding = it.codeFirstRep.asCode()
        else
          it.extension.expressionExtension()!!.run {
            valueReference =
              fhirPathEngine
                .evaluate(
                  null,
                  focusResource,
                  null,
                  null,
                  this.castToExpression(this.value).expression
                )
                .firstOrNull()
                .toString()
          }
      }
    }

  suspend fun searchResourceFor(
    resourceType: ResourceType,
    dataRequirement: DataRequirement,
    focusResource: Resource? = null,
    currentPage: Int = 0,
    limit: Int = PaginationConstant.DEFAULT_QUERY_LOAD_SIZE
  ): List<Resource> =
    searchResource(resourceType, dataRequirement.asSearchFilter(focusResource), currentPage, limit)

  suspend fun searchResource(
    resourceType: ResourceType,
    filters: List<SearchFilter> = listOf(),
    currentPage: Int = 0,
    limit: Int = PaginationConstant.DEFAULT_QUERY_LOAD_SIZE
  ): List<Resource> =
    when (resourceType) {
      ResourceType.Patient -> searchResource<Patient>(filters, currentPage, limit)
      ResourceType.CarePlan -> searchResource<CarePlan>(filters, currentPage, limit)
      ResourceType.Flag -> searchResource<Flag>(filters, currentPage, limit)
      ResourceType.Condition -> searchResource<Condition>(filters, currentPage, limit)
      ResourceType.Encounter -> searchResource<Encounter>(filters, currentPage, limit)
      ResourceType.Observation -> searchResource<Observation>(filters, currentPage, limit)
      ResourceType.Medication -> searchResource<Medication>(filters, currentPage, limit)
      ResourceType.Task -> searchResource<Task>(filters, currentPage, limit)
      else ->
        throw UnsupportedOperationException(
          "${resourceType.name} as register data filters is not supported yet"
        )
    }

  suspend inline fun <reified T : Resource> searchResource(
    filters: List<SearchFilter> = listOf(),
    currentPage: Int = 0,
    limit: Int = PaginationConstant.DEFAULT_QUERY_LOAD_SIZE
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filters.forEach { filterBy(it) }

        count = limit
        from = currentPage * limit
      }
    }

  suspend inline fun <reified T : Resource> searchResourceFor(
    subjectId: String,
    subjectType: ResourceType = ResourceType.Patient,
    subjectParam: ReferenceClientParam,
    filters: List<SearchFilter> = listOf()
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(subjectParam, subjectType, subjectId)

        filters.forEach { filterBy(it) }
      }
    }

  suspend fun loadDataForParam(
    param: Parameters.ParametersParameterComponent,
    focusResource: Resource?,
    currentPage: Int = 0,
    limit: Int = DEFAULT_MAX_PAGE_COUNT
  ): List<RegisterData.RawRegisterData> {
    param.let { param ->
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
          loadValueData(param, focusResource, currentPage, limit).map {
            RegisterData.RawRegisterData(
              id = it.logicalId,
              name = it.logicalId,
              module = param.name,
              main = Pair(param.name, it)
            )
          }
        param.hasResource() -> throw UnsupportedOperationException("Dont know resource handling")
        else -> {
          // for parts defined the config must define the mapping for main param
          val main =
            param.part.filter { it.name == param.name }.flatMap { mainParam ->
              loadValueData(mainParam, focusResource)
            }

          main.map {
            // add main to context data
            val contextData = mutableMapOf<String, Any>(param.name to it)

            val details = mutableMapOf<String, MutableList<Resource>>()

            param.part.filter { it.name != param.name }.forEach { partParam ->
              loadValueData(partParam, it)
                .apply {
                  if (details[partParam.name] == null)
                    details[partParam.name] = this.toMutableList()
                  else details[partParam.name]!!.addAll(this)
                }
                .also {
                  // add subsequent items to context data
                  contextData[partParam.name] = it
                }
            }

            RegisterData.RawRegisterData(
              id = it.logicalId,
              name = it.logicalId,
              module = param.name,
              main = Pair(param.name, it),
              _details = details.toMutableMap()
            )
          }
        }
      }
    }
  }

  suspend fun loadValueData(
    param: Parameters.ParametersParameterComponent,
    focusResource: Resource?,
    currentPage: Int = 0,
    limit: Int = DEFAULT_MAX_PAGE_COUNT
  ): List<Resource> {
    if (param.value.fhirType() == Enumerations.FHIRAllTypes.ID.toCode())
      return listOf() // TODO????????
    with(param.castToDataRequirement(param.value!!)) {
      return searchResourceFor(
        resourceType = ResourceType.fromCode(this.type),
        dataRequirement = this,
        focusResource = focusResource,
        currentPage = currentPage,
        limit = if (this.limit > 0) this.limit else limit // Bug in FHIR where for null it returns 0
      )
    }
  }

  suspend fun countDataForParam(param: Parameters.ParametersParameterComponent): Long {
    return fhirEngine.count<Patient> {
      param
        .let { mainParam ->
          if (mainParam.hasValue()) listOf(mainParam.value!!)
          else mainParam.part.filter { it.name == mainParam.name }
        }
        .flatMap { it.castToDataRequirement(it).asSearchFilter(fhirPathEngine) }
        .forEach { filterBy(it) }
    }
  }

  suspend fun searchQuestionnaireConfig(
    filters: List<SearchFilter> = listOf()
  ): List<QuestionnaireConfig> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search<Questionnaire> { filters.forEach { filterBy(it) } }.map {
        QuestionnaireConfig(
          form = it.nameElement.getLocalizedText() ?: it.logicalId,
          title = it.titleElement.getLocalizedText()
              ?: it.nameElement.getLocalizedText() ?: it.logicalId,
          identifier = it.logicalId
        )
      }
    }

  suspend fun loadConditions(
    patientId: String,
    filters: List<SearchFilter> = listOf()
  ): List<Condition> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, patientId)

        filters.forEach { filterBy(it) }
      }
    }

  suspend fun search(dataRequirement: DataRequirement) =
    when (dataRequirement.type) {
      Enumerations.ResourceType.CONDITION.toCode() ->
        fhirEngine.search<Condition> {
          dataRequirement.codeFilter.forEach {
            filter(TokenClientParam(it.path), { value = of(it.codeFirstRep) })
          }
          // TODO handle date filter
        }
      else -> listOf()
    }

  suspend fun searchCompositionByIdentifier(identifier: String): Composition? =
    fhirEngine
      .search<Composition> {
        filter(Composition.IDENTIFIER, { value = of(Identifier().apply { value = identifier }) })
      }
      .firstOrNull()

  suspend fun getBinary(id: String): Binary = fhirEngine.load(Binary::class.java, id)

  suspend fun save(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      resource.generateMissingId()
      fhirEngine.save(resource)
    }
  }

  suspend fun delete(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      fhirEngine.remove(resource::class.java, resource.logicalId)
    }
  }

  suspend fun <R : Resource> addOrUpdate(resource: R) {
    return withContext(dispatcherProvider.io()) {
      try {
        fhirEngine.load(resource::class.java, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        resource.generateMissingId()
        fhirEngine.save(resource)
      }
    }
  }
}
