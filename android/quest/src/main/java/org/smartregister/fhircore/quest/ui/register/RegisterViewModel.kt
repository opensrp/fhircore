/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.google.android.fhir.sync.CurrentSyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterFilterField
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.NestedSearchConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState
import org.smartregister.fhircore.quest.util.extensions.toParamDataMap
import timber.log.Timber

@HiltViewModel
class RegisterViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
) : ViewModel() {

  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()
  val registerUiState = mutableStateOf(RegisterUiState())
  val currentPage: MutableState<Int> = mutableIntStateOf(0)
  val registerData: MutableStateFlow<Flow<PagingData<ResourceData>>> = MutableStateFlow(emptyFlow())
  val pagesDataCache = mutableMapOf<Int, Flow<PagingData<ResourceData>>>()
  val registerFilterState = mutableStateOf(RegisterFilterState())
  private val _totalRecordsCount = mutableLongStateOf(0L)
  private val _filteredRecordsCount = mutableLongStateOf(-1L)
  private lateinit var registerConfiguration: RegisterConfiguration
  private var completeRegisterData: Flow<PagingData<ResourceData>>? = null
  private val _percentageProgress: MutableSharedFlow<Int> = MutableSharedFlow(0)
  private val _isUploadSync: MutableSharedFlow<Boolean> = MutableSharedFlow(0)
  private val _currentSyncJobStatusFlow: MutableSharedFlow<CurrentSyncJobStatus?> =
    MutableSharedFlow(0)
  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application, paramsMap = emptyMap())
  }

  /**
   * This function paginates the register data. An optional [clearCache] resets the data in the
   * cache (this is necessary after a questionnaire has been submitted to refresh the register with
   * new/updated data).
   */
  fun paginateRegisterData(
    registerId: String,
    loadAll: Boolean = false,
    clearCache: Boolean = false,
  ) {
    if (clearCache) {
      pagesDataCache.clear()
      completeRegisterData = null
    }
    registerData.value =
      pagesDataCache.getOrPut(currentPage.value) {
        getPager(registerId, loadAll).flow.cachedIn(viewModelScope)
      }
  }

  private fun getPager(registerId: String, loadAll: Boolean = false): Pager<Int, ResourceData> {
    val currentRegisterConfigs = retrieveRegisterConfiguration(registerId)
    val ruleConfigs = currentRegisterConfigs.registerCard.rules
    val pageSize = currentRegisterConfigs.pageSize

    return Pager(
      config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
      pagingSourceFactory = {
        RegisterPagingSource(
            registerRepository = registerRepository,
            resourceDataRulesExecutor = resourceDataRulesExecutor,
            ruleConfigs = ruleConfigs,
            fhirResourceConfig = registerFilterState.value.fhirResourceConfig,
            actionParameters = registerUiState.value.params,
          )
          .apply {
            setPatientPagingSourceState(
              RegisterPagingSourceState(
                registerId = registerId,
                loadAll = loadAll,
                currentPage = if (loadAll) 0 else currentPage.value,
              ),
            )
          }
      },
    )
  }

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramMap: Map<String, String>? = emptyMap(),
  ): RegisterConfiguration {
    // Ensures register configuration is initialized once
    if (!::registerConfiguration.isInitialized) {
      registerConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramMap)
    }
    return registerConfiguration
  }

  private fun retrieveCompleteRegisterData(
    registerId: String,
    forceRefresh: Boolean,
  ): Flow<PagingData<ResourceData>> {
    if (completeRegisterData == null || forceRefresh) {
      completeRegisterData = getPager(registerId, true).flow.cachedIn(viewModelScope)
    }
    return completeRegisterData!!
  }

  fun onEvent(event: RegisterEvent) {
    val registerId = registerUiState.value.registerId
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is RegisterEvent.SearchRegister -> {
        if (event.searchQuery.isBlank()) {
          val regConfig = retrieveRegisterConfiguration(registerId)
          if (regConfig.infiniteScroll) {
            registerData.value = retrieveCompleteRegisterData(registerId, false)
          } else {
            paginateRegisterData(registerId)
          }
        } else {
          filterRegisterData(event.searchQuery.query)
        }
      }
      is RegisterEvent.MoveToNextPage -> {
        currentPage.value = currentPage.value.plus(1)
        paginateRegisterData(registerId)
      }
      is RegisterEvent.MoveToPreviousPage -> {
        currentPage.value.let { if (it > 0) currentPage.value = it.minus(1) }
        paginateRegisterData(registerId)
      }
      RegisterEvent.ResetFilterRecordsCount -> _filteredRecordsCount.longValue = -1
    }
  }

  fun filterRegisterData(searchText: String) {
    val searchBar = registerUiState.value.registerConfiguration?.searchBar
    // computedRules (names of pre-computed rules) must be provided for search to work.
    if (searchBar?.computedRules != null) {
      registerData.value =
        retrieveCompleteRegisterData(registerUiState.value.registerId, false).map {
          pagingData: PagingData<ResourceData> ->
          pagingData.filter { resourceData: ResourceData ->
            searchBar.computedRules!!.any { ruleName ->
              // if ruleName not found in map return {-1}; check always return false hence no data
              val value = resourceData.computedValuesMap[ruleName]?.toString() ?: "{-1}"
              value.contains(other = searchText, ignoreCase = true)
            }
          }
        }
    }
  }

  fun updateRegisterFilterState(registerId: String, questionnaireResponse: QuestionnaireResponse) {
    // Reset filter state if no answer is provided for all the fields
    if (questionnaireResponse.item.all { !it.hasAnswer() }) {
      registerFilterState.value =
        RegisterFilterState(
          questionnaireResponse = null,
          fhirResourceConfig = null,
        )
      return
    }

    val registerConfiguration = retrieveRegisterConfiguration(registerId)
    val resourceConfig = registerConfiguration.fhirResource
    val baseResource = resourceConfig.baseResource
    val qrItemMap = questionnaireResponse.item.groupBy { it.linkId }.mapValues { it.value.first() }

    val registerDataFilterFieldsMap =
      registerConfiguration.registerFilter
        ?.dataFilterFields
        ?.groupBy { it.filterId }
        ?.mapValues { it.value.first() }

    // Get filter queries from the map. NOTE: filterId MUST be unique for all resources
    val baseResourceRegisterFilterField = registerDataFilterFieldsMap?.get(baseResource.filterId)
    val newBaseResourceDataQueries =
      createQueriesForRegisterFilter(
        dataQueries = baseResourceRegisterFilterField?.dataQueries,
        qrItemMap = qrItemMap,
      )

    val newRelatedResources =
      createFilterRelatedResources(
        registerDataFilterFieldsMap = registerDataFilterFieldsMap,
        relatedResources = resourceConfig.relatedResources,
        qrItemMap = qrItemMap,
      )

    val fhirResourceConfig =
      FhirResourceConfig(
        baseResource =
          baseResource.copy(
            dataQueries = newBaseResourceDataQueries ?: baseResource.dataQueries,
            nestedSearchResources =
              getValidatedNestedSearchResources(
                  baseResourceRegisterFilterField?.nestedSearchResources,
                  qrItemMap,
                )
                ?.map { nestedSearchConfig ->
                  nestedSearchConfig.copy(
                    dataQueries =
                      createQueriesForRegisterFilter(
                        dataQueries = nestedSearchConfig.dataQueries,
                        qrItemMap = qrItemMap,
                      ),
                  )
                } ?: baseResource.nestedSearchResources,
          ),
        relatedResources = newRelatedResources,
      )
    registerFilterState.value =
      RegisterFilterState(
        questionnaireResponse = questionnaireResponse,
        fhirResourceConfig = fhirResourceConfig,
      )
    Timber.i("New ResourceConfig for register data filter: ${fhirResourceConfig.encodeJson()}")
  }

  private fun getValidatedNestedSearchResources(
    nestedSearchResources: List<NestedSearchConfig>?,
    qrItemMap: Map<String, QuestionnaireResponse.QuestionnaireResponseItemComponent>,
  ) =
    nestedSearchResources?.filter { nestedSearchConfig ->
      nestedSearchConfig.dataQueries?.any { dataQuery ->
        dataQuery.filterCriteria.any { filterCriterionConfig ->
          filterCriterionConfig.dataFilterLinkId.isNullOrEmpty() ||
            qrItemMap[filterCriterionConfig.dataFilterLinkId]?.answer?.isNotEmpty() == true
        }
      } ?: false
    }

  private fun createFilterRelatedResources(
    registerDataFilterFieldsMap: Map<String, RegisterFilterField>?,
    relatedResources: List<ResourceConfig>,
    qrItemMap: Map<String, QuestionnaireResponse.QuestionnaireResponseItemComponent>,
  ): List<ResourceConfig> {
    val newRelatedResources =
      relatedResources.map { resourceConfig: ResourceConfig ->
        val registerFilterField = registerDataFilterFieldsMap?.get(resourceConfig.filterId)
        val newDataQueries =
          createQueriesForRegisterFilter(
            dataQueries = registerFilterField?.dataQueries,
            qrItemMap = qrItemMap,
          )
        resourceConfig.copy(
          dataQueries = newDataQueries ?: resourceConfig.dataQueries,
          relatedResources =
            createFilterRelatedResources(
              registerDataFilterFieldsMap = registerDataFilterFieldsMap,
              relatedResources = resourceConfig.relatedResources,
              qrItemMap = qrItemMap,
            ),
          nestedSearchResources =
            registerFilterField?.nestedSearchResources?.map { nestedSearchConfig ->
              nestedSearchConfig.copy(
                dataQueries =
                  createQueriesForRegisterFilter(
                    dataQueries = nestedSearchConfig.dataQueries,
                    qrItemMap = qrItemMap,
                  ),
              )
            } ?: resourceConfig.nestedSearchResources,
        )
      }
    return newRelatedResources
  }

  private fun createQueriesForRegisterFilter(
    dataQueries: List<DataQuery>?,
    qrItemMap: Map<String, QuestionnaireResponse.QuestionnaireResponseItemComponent>,
  ) =
    dataQueries?.map {
      val newFilterCriteria = mutableListOf<FilterCriterionConfig>()
      it.filterCriteria.forEach { filterCriterionConfig ->
        if (!filterCriterionConfig.dataFilterLinkId.isNullOrEmpty()) {
          val answerComponent = qrItemMap[filterCriterionConfig.dataFilterLinkId]
          answerComponent?.answer?.forEach { itemAnswerComponent ->
            val criterion =
              convertAnswerToFilterCriterion(itemAnswerComponent, filterCriterionConfig)
            if (criterion != null) newFilterCriteria.add(criterion)
          }
        } else {
          newFilterCriteria.add(filterCriterionConfig)
        }
      }
      it.copy(filterCriteria = newFilterCriteria)
    }

  private fun convertAnswerToFilterCriterion(
    answerComponent: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent,
    oldFilterCriterion: FilterCriterionConfig,
  ): FilterCriterionConfig? =
    when {
      answerComponent.hasValueCoding() -> {
        val valueCoding: Coding = answerComponent.valueCoding
        FilterCriterionConfig.TokenFilterCriterionConfig(
          dataType = DataType.CODE,
          computedRule = oldFilterCriterion.computedRule,
          value = Code(valueCoding.system, valueCoding.code, valueCoding.display),
        )
      }
      answerComponent.hasValueStringType() -> {
        val stringFilterCriterion =
          oldFilterCriterion as FilterCriterionConfig.StringFilterCriterionConfig
        FilterCriterionConfig.StringFilterCriterionConfig(
          dataType = DataType.STRING,
          computedRule = stringFilterCriterion.computedRule,
          modifier = stringFilterCriterion.modifier,
          value = answerComponent.valueStringType.value,
        )
      }
      answerComponent.hasValueQuantity() -> {
        val quantityCriteria =
          oldFilterCriterion as FilterCriterionConfig.QuantityFilterCriterionConfig
        FilterCriterionConfig.QuantityFilterCriterionConfig(
          dataType = DataType.QUANTITY,
          computedRule = quantityCriteria.computedRule,
          prefix = quantityCriteria.prefix,
          system = quantityCriteria.system,
          unit = quantityCriteria.unit,
          value = answerComponent.valueDecimalType.value,
        )
      }
      answerComponent.hasValueIntegerType() -> {
        val numberFilterCriterion =
          oldFilterCriterion as FilterCriterionConfig.NumberFilterCriterionConfig
        FilterCriterionConfig.NumberFilterCriterionConfig(
          dataType = DataType.INTEGER,
          computedRule = numberFilterCriterion.computedRule,
          prefix = numberFilterCriterion.prefix,
          value = answerComponent.valueIntegerType.value.toBigDecimal(),
        )
      }
      answerComponent.hasValueDecimalType() -> {
        val numberFilterCriterion =
          oldFilterCriterion as FilterCriterionConfig.NumberFilterCriterionConfig
        FilterCriterionConfig.NumberFilterCriterionConfig(
          dataType = DataType.DECIMAL,
          computedRule = numberFilterCriterion.computedRule,
          prefix = numberFilterCriterion.prefix,
          value = answerComponent.valueDecimalType.value,
        )
      }
      answerComponent.hasValueDateTimeType() -> {
        val dateFilterCriterion =
          oldFilterCriterion as FilterCriterionConfig.DateFilterCriterionConfig
        FilterCriterionConfig.DateFilterCriterionConfig(
          dataType = DataType.DATETIME,
          computedRule = dateFilterCriterion.computedRule,
          prefix = dateFilterCriterion.prefix,
          valueAsDateTime = true,
          value = answerComponent.valueDateTimeType.asStringValue(),
        )
      }
      answerComponent.hasValueDateType() -> {
        val dateFilterCriterion =
          oldFilterCriterion as FilterCriterionConfig.DateFilterCriterionConfig
        FilterCriterionConfig.DateFilterCriterionConfig(
          dataType = DataType.DATE,
          computedRule = dateFilterCriterion.computedRule,
          prefix = dateFilterCriterion.prefix,
          valueAsDateTime = dateFilterCriterion.valueAsDateTime,
          value = answerComponent.valueDateType.asStringValue(),
        )
      }
      answerComponent.hasValueUriType() -> {
        val uriCriterion = oldFilterCriterion as FilterCriterionConfig.UriFilterCriterionConfig
        FilterCriterionConfig.UriFilterCriterionConfig(
          dataType = DataType.URI,
          computedRule = uriCriterion.computedRule,
          value = answerComponent.valueUriType.valueAsString,
        )
      }
      answerComponent.hasValueReference() -> {
        val referenceCriterion =
          oldFilterCriterion as FilterCriterionConfig.ReferenceFilterCriterionConfig
        FilterCriterionConfig.ReferenceFilterCriterionConfig(
          dataType = DataType.REFERENCE,
          computedRule = referenceCriterion.computedRule,
          value = answerComponent.valueReference.reference,
        )
      }
      else -> {
        null
      }
    }

  fun retrieveRegisterUiState(
    registerId: String,
    screenTitle: String,
    params: Array<ActionParameter>? = emptyArray(),
    clearCache: Boolean,
  ) {
    if (registerId.isNotEmpty()) {
      val paramsMap: Map<String, String> = params.toParamDataMap()
      viewModelScope.launch {
        val currentRegisterConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
        if (currentRegisterConfiguration.infiniteScroll) {
          registerData.value =
            retrieveCompleteRegisterData(currentRegisterConfiguration.id, clearCache)
        } else {
          _totalRecordsCount.longValue =
            registerRepository.countRegisterData(registerId = registerId, paramsMap = paramsMap)

          // Only count filtered data when queries are updated
          if (registerFilterState.value.fhirResourceConfig != null) {
            _filteredRecordsCount.longValue =
              registerRepository.countRegisterData(
                registerId = registerId,
                paramsMap = paramsMap,
                fhirResourceConfig = registerFilterState.value.fhirResourceConfig,
              )
          }
          paginateRegisterData(registerId = registerId, loadAll = false, clearCache = clearCache)
        }

        registerUiState.value =
          RegisterUiState(
            screenTitle = currentRegisterConfiguration.registerTitle ?: screenTitle,
            isFirstTimeSync =
              sharedPreferencesHelper
                .read(
                  SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name,
                  null,
                )
                .isNullOrEmpty() &&
                _totalRecordsCount.longValue == 0L &&
                applicationConfiguration.usePractitionerAssignedLocationOnSync,
            registerConfiguration = currentRegisterConfiguration,
            registerId = registerId,
            totalRecordsCount = _totalRecordsCount.longValue,
            filteredRecordsCount = _filteredRecordsCount.longValue,
            pagesCount =
              ceil(
                  (if (registerFilterState.value.fhirResourceConfig != null) {
                      _filteredRecordsCount.longValue
                    } else {
                      _totalRecordsCount.longValue
                    })
                    .toDouble()
                    .div(currentRegisterConfiguration.pageSize.toLong()),
                )
                .toInt(),
            progressPercentage = _percentageProgress,
            isSyncUpload = _isUploadSync,
            currentSyncJobStatus = _currentSyncJobStatusFlow,
            params = paramsMap,
          )
      }
    }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }
}
