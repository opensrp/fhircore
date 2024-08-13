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

package org.smartregister.fhircore.quest.ui.geowidget

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonPrimitive
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.Geometry
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler

@HiltViewModel
class GeoWidgetLauncherViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  val configurationRegistry: ConfigurationRegistry,
) : ViewModel() {

  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

  private val _noLocationFoundDialog = MutableLiveData<Boolean>()
  val noLocationFoundDialog: LiveData<Boolean>
    get() = _noLocationFoundDialog

  private val applicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
  }
  private lateinit var repositoryResourceDataList: List<RepositoryResourceData>

  suspend fun retrieveLocations(
    geoWidgetConfig: GeoWidgetConfiguration,
    searchText: String? = null,
  ): List<GeoJsonFeature> {
    val geoJsonFeatures = mutableListOf<GeoJsonFeature>()
    val repositoryResourceDataList = retrieveResources(geoWidgetConfig)

    repositoryResourceDataList.forEach { repositoryResourceData ->
      val location = repositoryResourceData.resource as Location
      val resourceData =
        resourceDataRulesExecutor.processResourceData(
          repositoryResourceData = repositoryResourceData,
          ruleConfigs = geoWidgetConfig.servicePointConfig?.rules ?: emptyList(),
          params = emptyMap(),
        )
      val servicePointProperties = mutableMapOf<String, JsonPrimitive>()
      geoWidgetConfig.servicePointConfig?.servicePointProperties?.forEach { (key, value) ->
        servicePointProperties[key] =
          JsonPrimitive(value.interpolate(resourceData.computedValuesMap))
      }

      if (
        location.hasPosition() &&
          location.position.hasLatitude() &&
          location.position.hasLongitude()
      ) {
        val feature =
          GeoJsonFeature(
            id = location.idElement.idPart,
            geometry =
              Geometry(
                coordinates = // MapBox coordinates are represented as Long,Lat (NOT Lat,Long)
                listOf(
                    location.position.longitude.toDouble(),
                    location.position.latitude.toDouble(),
                  ),
              ),
            properties = servicePointProperties,
          )

        val keys = geoWidgetConfig.topScreenSection?.searchBar?.computedRules

        if (!keys.isNullOrEmpty() && !searchText.isNullOrEmpty()) {
          val addFeature =
            keys.any { key ->
              servicePointProperties[key].toString().contains(other = searchText, ignoreCase = true)
            }
          if (addFeature) {
            geoJsonFeatures.add(feature)
          }
        }

        if (searchText.isNullOrEmpty()) {
          geoJsonFeatures.add(feature)
        }
      }
    }
    return geoJsonFeatures
  }

  fun showNoLocationDialog(geoWidgetConfiguration: GeoWidgetConfiguration) {
    geoWidgetConfiguration.noResults?.let { _noLocationFoundDialog.postValue(true) }
  }

  suspend fun retrieveResources(
    geoWidgetConfig: GeoWidgetConfiguration,
  ): List<RepositoryResourceData> {
    if (!this::repositoryResourceDataList.isInitialized) {
      repositoryResourceDataList =
        defaultRepository.searchResourcesRecursively(
          filterActiveResources = null,
          fhirResourceConfig = geoWidgetConfig.resourceConfig,
          configRules = null,
          secondaryResourceConfigs = null,
          filterByRelatedEntityLocationMetaTag =
            geoWidgetConfig.filterDataByRelatedEntityLocation == true,
        )
    }
    return repositoryResourceDataList
  }

  suspend fun onQuestionnaireSubmission(
    extractedResourceIds: List<IdType>,
    emitFeature: (GeoJsonFeature) -> Unit,
  ) {
    val locationId =
      extractedResourceIds.firstOrNull { it.resourceType == ResourceType.Location.name } ?: return
    val location =
      defaultRepository.loadResource<Location>(locationId.valueAsString.extractLogicalIdUuid())
        ?: return

    val feature =
      GeoJsonFeature(
        id = location.id,
        geometry =
          Geometry(
            coordinates = // MapBox coordinates are represented as Long,Lat (NOT Lat,Long)
            listOf(
                location.position.longitude.toDouble(),
                location.position.latitude.toDouble(),
              ),
          ),
      )
    emitFeature(feature)
  }

  fun launchQuestionnaire(
    questionnaireConfig: QuestionnaireConfig,
    feature: GeoJsonFeature,
    context: Context,
  ) {
    val params =
      addMatchingCoordinatesToActionParameters(
        feature.geometry?.coordinates?.get(0),
        feature.geometry?.coordinates?.get(1),
        questionnaireConfig.extraParams,
      )
    if (context is QuestionnaireHandler) {
      context.launchQuestionnaire(
        context = context,
        questionnaireConfig = questionnaireConfig,
        actionParams = params,
      )
    }
  }

  /**
   * Adds coordinates into the correct action parameter as [ActionParameter.value] if the
   * [ActionParameter.key] matches with [KEY_LATITUDE] or [KEY_LONGITUDE] constants. *
   */
  private fun addMatchingCoordinatesToActionParameters(
    latitude: Double?,
    longitude: Double?,
    params: List<ActionParameter>?,
  ): List<ActionParameter> {
    if (latitude == null || longitude == null) {
      throw IllegalArgumentException("Latitude or Longitude must not be null")
    }
    params ?: return emptyList()
    return params
      .filter {
        it.paramType == ActionParameterType.PREPOPULATE &&
          it.dataType == Enumerations.DataType.STRING
      }
      .map {
        return@map when (it.key) {
          KEY_LATITUDE -> it.copy(value = latitude.toString())
          KEY_LONGITUDE -> it.copy(value = longitude.toString())
          else -> it
        }
      }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }

  fun isFirstTime(): Boolean =
    sharedPreferencesHelper
      .read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
      .isNullOrEmpty() && applicationConfiguration.usePractitionerAssignedLocationOnSync

  private companion object {
    const val KEY_LATITUDE = "positionLatitude"
    const val KEY_LONGITUDE = "positionLongitude"
  }
}
