/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterFilterField
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
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
  val dispatcherProvider: DispatcherProvider,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
) : ViewModel() {

  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()
  val registerUiState = mutableStateOf(RegisterUiState())
  val currentPage: MutableState<Int> = mutableIntStateOf(0)
  val searchText = mutableStateOf("")
  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<ResourceData>>> =
    MutableStateFlow(emptyFlow())
  val pagesDataCache = mutableMapOf<Int, Flow<PagingData<ResourceData>>>()
  private val registerFilterState = mutableStateOf(RegisterFilterState())
  private val _totalRecordsCount = mutableLongStateOf(0L)
  private lateinit var registerConfiguration: RegisterConfiguration
  private var allPatientRegisterData: Flow<PagingData<ResourceData>>? = null
  private val _percentageProgress: MutableSharedFlow<Int> = MutableSharedFlow(0)
  private val _isUploadSync: MutableSharedFlow<Boolean> = MutableSharedFlow(0)

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
      allPatientRegisterData = null
    }
    paginatedRegisterData.value =
      pagesDataCache.getOrPut(currentPage.value) {
        getPager(registerId, loadAll).flow.cachedIn(viewModelScope)
      }
  }

  private fun getPager(registerId: String, loadAll: Boolean = false): Pager<Int, ResourceData> {
    val currentRegisterConfigs = retrieveRegisterConfiguration(registerId)
    val ruleConfigs = currentRegisterConfigs.registerCard.rules
    val pageSize = currentRegisterConfigs.pageSize // Default 10

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

  private fun retrieveAllPatientRegisterData(registerId: String): Flow<PagingData<ResourceData>> {
    // Ensure that we only initialize this flow once
    if (allPatientRegisterData == null) {
      allPatientRegisterData = getPager(registerId, true).flow.cachedIn(viewModelScope)
    }
    return allPatientRegisterData!!
  }

  fun onEvent(event: RegisterEvent) =
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is RegisterEvent.SearchRegister -> {
        searchText.value = event.searchText
        if (event.searchText.isEmpty()) {
          paginateRegisterData(registerUiState.value.registerId)
        } else {
          filterRegisterData(event)
        }
      }
      is RegisterEvent.MoveToNextPage -> {
        currentPage.value = currentPage.value.plus(1)
        paginateRegisterData(registerUiState.value.registerId)
      }
      is RegisterEvent.MoveToPreviousPage -> {
        currentPage.value.let { if (it > 0) currentPage.value = it.minus(1) }
        paginateRegisterData(registerUiState.value.registerId)
      }
    }

  fun filterRegisterData(event: RegisterEvent.SearchRegister) {
    val searchBar = registerUiState.value.registerConfiguration?.searchBar
    // computedRules (names of pre-computed rules) must be provided for search to work.
    if (searchBar?.computedRules != null) {
      paginatedRegisterData.value =
        retrieveAllPatientRegisterData(registerUiState.value.registerId).map {
          pagingData: PagingData<ResourceData> ->
          pagingData.filter { resourceData: ResourceData ->
            searchBar.computedRules!!.any { ruleName ->
              // if ruleName not found in map return {-1}; check always return false hence no data
              val value = resourceData.computedValuesMap[ruleName]?.toString() ?: "{-1}"
              value.contains(other = event.searchText, ignoreCase = true)
            }
          }
        }
    }
  }

  fun updateRegisterFilterState(registerId: String, questionnaireResponse: QuestionnaireResponse) {
    val registerConfiguration = retrieveRegisterConfiguration(registerId)
    val resourceConfig = registerConfiguration.fhirResource
    val registerFilterFields: List<RegisterFilterField>? =
      registerConfiguration.registerFilter?.dataFilterFields
    val qrItemMap = questionnaireResponse.item.groupBy { it.linkId }

    var newFhirResource = FhirResourceConfig(resourceConfig.baseResource)

    // TODO Where should the filterFieldLinkId be put? FilterCriterion or DataQuery
    registerFilterFields?.forEach { filterField ->
      if (!filterField.validate()) {
        Timber.e(
          "Invalid register filter data query $filterField. Every data query MUST have FilterCriteriaConfig",
        )
        return
      }
      val newDataQueries =
        filterField.dataQueries.map { dataQuery ->
          val newFilterCriteria = mutableListOf<FilterCriterionConfig>()
          if (qrItemMap.containsKey(dataQuery.filterFieldLinkId)) {
            val qrItem =
              qrItemMap
                .getValue(dataQuery.filterFieldLinkId)
                .first<QuestionnaireResponse.QuestionnaireResponseItemComponent?>()

            qrItem?.answer?.forEach { answerComponent ->

              // Every item in DataQuery.filterCriteria should look the same apart from the value
              val firstFilterCriterion: FilterCriterionConfig = dataQuery.filterCriteria.first()
              when {
                answerComponent.hasValueCoding() -> {
                  val valueCoding: Coding = answerComponent.valueCoding
                  FilterCriterionConfig.TokenFilterCriterionConfig(
                    dataType = firstFilterCriterion.dataType,
                    computedRule = firstFilterCriterion.computedRule,
                    value = Code(valueCoding.system, valueCoding.code, valueCoding.display),
                  )
                }
                answerComponent.hasValueStringType() -> {
                  val stringFilterCriterion =
                    firstFilterCriterion as FilterCriterionConfig.StringFilterCriterionConfig
                  FilterCriterionConfig.StringFilterCriterionConfig(
                    dataType = DataType.STRING,
                    computedRule = stringFilterCriterion.computedRule,
                    modifier = stringFilterCriterion.modifier,
                    value = answerComponent.valueStringType.value,
                  )
                }
                answerComponent.hasValueQuantity() -> {
                  val quantityCriteria =
                    firstFilterCriterion as FilterCriterionConfig.QuantityFilterCriterionConfig
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
                    firstFilterCriterion as FilterCriterionConfig.NumberFilterCriterionConfig
                  FilterCriterionConfig.NumberFilterCriterionConfig(
                    dataType = DataType.DECIMAL,
                    computedRule = numberFilterCriterion.computedRule,
                    prefix = numberFilterCriterion.prefix,
                    value = answerComponent.valueIntegerType.value.toBigDecimal(),
                  )
                }
                answerComponent.hasValueDecimalType() -> {
                  val numberFilterCriterion =
                    firstFilterCriterion as FilterCriterionConfig.NumberFilterCriterionConfig
                  FilterCriterionConfig.NumberFilterCriterionConfig(
                    dataType = DataType.DECIMAL,
                    computedRule = numberFilterCriterion.computedRule,
                    prefix = numberFilterCriterion.prefix,
                    value = answerComponent.valueDecimalType.value,
                  )
                }
                answerComponent.hasValueDateTimeType() -> {
                  val dateFilterCriterion =
                    firstFilterCriterion as FilterCriterionConfig.DateFilterCriterionConfig
                  FilterCriterionConfig.DateFilterCriterionConfig(
                    dataType = DataType.DATETIME,
                    computedRule = dateFilterCriterion.computedRule,
                    prefix = dateFilterCriterion.prefix,
                    valueAsDateTime = true,
                    value = answerComponent.valueDecimalType.asStringValue(),
                  )
                }
                answerComponent.hasValueDateType() -> {
                  val dateFilterCriterion =
                    firstFilterCriterion as FilterCriterionConfig.DateFilterCriterionConfig
                  FilterCriterionConfig.DateFilterCriterionConfig(
                    dataType = DataType.DATE,
                    computedRule = dateFilterCriterion.computedRule,
                    prefix = dateFilterCriterion.prefix,
                    valueAsDateTime = false,
                    value = answerComponent.valueDateType.asStringValue(),
                  )
                }
                answerComponent.hasValueUriType() -> {
                  val uriCriterion =
                    firstFilterCriterion as FilterCriterionConfig.UriFilterCriterionConfig
                  FilterCriterionConfig.UriFilterCriterionConfig(
                    dataType = DataType.URI,
                    computedRule = uriCriterion.computedRule,
                    value = answerComponent.valueUriType.valueAsString,
                  )
                }
                answerComponent.hasValueReference() -> {
                  val referenceCriterion =
                    firstFilterCriterion as FilterCriterionConfig.ReferenceFilterCriterionConfig
                  FilterCriterionConfig.ReferenceFilterCriterionConfig(
                    dataType = DataType.REFERENCE,
                    computedRule = referenceCriterion.computedRule,
                    value = answerComponent.valueReference.reference,
                  )
                }
                else -> {
                  null
                }
              }?.also { newFilterCriteria.add(it) }
            }
          }
          DataQuery(
            paramName = dataQuery.paramName,
            operation = dataQuery.operation,
            filterFieldLinkId = dataQuery.filterFieldLinkId,
            filterCriteria = newFilterCriteria,
          )
        }

      newFhirResource =
        newFhirResource.copy(
          baseResource = newFhirResource.baseResource.copy(dataQueries = newDataQueries),
        )
    }

    registerFilterState.value =
      RegisterFilterState(
        questionnaireResponse = questionnaireResponse,
        fhirResourceConfig = newFhirResource,
      )
  }

  private fun RegisterFilterField.validate(): Boolean =
    this.dataQueries.all { dataQuery -> dataQuery.filterCriteria.isNotEmpty() }

  fun retrieveRegisterUiState(
    registerId: String,
    screenTitle: String,
    params: Array<ActionParameter>? = emptyArray(),
    clearCache: Boolean,
  ) {
    if (registerId.isNotEmpty()) {
      val paramsMap: Map<String, String> = params.toParamDataMap()
      viewModelScope.launch(dispatcherProvider.io()) {
        val currentRegisterConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
        // Count register data then paginate the data
        _totalRecordsCount.value =
          registerRepository.countRegisterData(
            registerId = registerId,
            paramsMap = paramsMap,
            fhirResourceConfig = registerFilterState.value.fhirResourceConfig,
          )

        paginateRegisterData(registerId, loadAll = false, clearCache = clearCache)

        registerUiState.value =
          RegisterUiState(
            screenTitle = currentRegisterConfiguration.registerTitle ?: screenTitle,
            isFirstTimeSync =
              sharedPreferencesHelper
                .read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
                .isNullOrEmpty() && _totalRecordsCount.value == 0L,
            registerConfiguration = currentRegisterConfiguration,
            registerId = registerId,
            totalRecordsCount = _totalRecordsCount.value,
            pagesCount =
              ceil(
                  _totalRecordsCount.value
                    .toDouble()
                    .div(currentRegisterConfiguration.pageSize.toLong()),
                )
                .toInt(),
            progressPercentage = _percentageProgress,
            isSyncUpload = _isUploadSync,
            params = paramsMap,
          )
      }
    }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }

  suspend fun emitPercentageProgressState(progress: Int, isUploadSync: Boolean) {
    _percentageProgress.emit(progress)
    _isUploadSync.emit(isUploadSync)
  }
}
