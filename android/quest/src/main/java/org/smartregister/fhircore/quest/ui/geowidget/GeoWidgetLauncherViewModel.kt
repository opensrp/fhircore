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
import android.graphics.Bitmap
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.retrieveRelatedEntitySyncLocationState
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.Geometry
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.util.extensions.referenceToBitmap
import timber.log.Timber

@HiltViewModel
class GeoWidgetLauncherViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  val configurationRegistry: ConfigurationRegistry,
  @ApplicationContext val context: Context,
) : ViewModel() {
  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

  private val _noLocationFoundDialog = MutableLiveData<Boolean>()
  val noLocationFoundDialog: LiveData<Boolean>
    get() = _noLocationFoundDialog

  private val applicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
  }

  private val decodedImageMap = mutableStateMapOf<String, Bitmap>()

  fun retrieveLocations(
    geoWidgetConfig: GeoWidgetConfiguration,
    searchText: String?,
    onReceiveData: (List<GeoJsonFeature>) -> Unit,
  ) {
    viewModelScope.launch {
      val totalCount =
        withContext(dispatcherProvider.io()) {
          defaultRepository.countResources(
            filterByRelatedEntityLocation =
              geoWidgetConfig.filterDataByRelatedEntityLocation == true,
            baseResourceConfig = geoWidgetConfig.resourceConfig.baseResource,
            filterActiveResources =
              listOf(
                ActiveResourceFilterConfig(
                  resourceType = ResourceType.Patient,
                  active = true,
                ),
                ActiveResourceFilterConfig(
                  resourceType = ResourceType.Group,
                  active = true,
                ),
              ),
            configComputedRuleValues = emptyMap(),
          )
        }
      if (totalCount == 0L) {
        showNoLocationDialog(geoWidgetConfig)
        return@launch
      }
      var count = 0
      var pageNumber = 0
      var locationsWithoutCoordinatesCount = 0L
      var registerDataCount = 0L
      while (count < totalCount) {
        val (locationsWithCoordinates, locationsWithoutCoordinates) =
          defaultRepository
            .searchResourcesRecursively(
              filterActiveResources = null,
              fhirResourceConfig = geoWidgetConfig.resourceConfig,
              configRules = null,
              secondaryResourceConfigs = null,
              filterByRelatedEntityLocationMetaTag =
                geoWidgetConfig.filterDataByRelatedEntityLocation == true,
              currentPage = pageNumber,
              pageSize = DefaultRepository.DEFAULT_BATCH_SIZE,
            )
            .asSequence()
            .filter { it.resource is Location }
            .partition {
              with((it.resource as Location).position) { hasLongitude() && hasLatitude() }
            }

        val registerData =
          locationsWithCoordinates
            .asSequence()
            .map {
              Pair(
                it.resource as Location,
                resourceDataRulesExecutor.processResourceData(
                  repositoryResourceData = it,
                  ruleConfigs = geoWidgetConfig.servicePointConfig?.rules ?: emptyList(),
                  params = emptyMap(),
                ),
              )
            }
            .map { (location, resourceData) ->
              GeoJsonFeature(
                id = location.logicalId,
                geometry =
                  Geometry(
                    coordinates = // MapBox coordinates are represented as Long,Lat (NOT Lat,Long)
                    listOf(
                        location.position.longitude.toDouble(),
                        location.position.latitude.toDouble(),
                      ),
                  ),
                properties =
                  geoWidgetConfig.servicePointConfig?.servicePointProperties?.mapValues {
                    JsonPrimitive(it.value.interpolate(resourceData.computedValuesMap))
                  } ?: emptyMap(),
              )
            }
            .toList()
        val geoJsonFeatures =
          if (searchText.isNullOrBlank()) {
            registerData
          } else {
            registerData.filter { geoJsonFeature: GeoJsonFeature ->
              geoWidgetConfig.topScreenSection?.searchBar?.computedRules?.any { ruleName ->
                // if ruleName not found in map return {-1}; check always return false hence no data
                val value = geoJsonFeature.properties[ruleName]?.toString() ?: "{-1}"
                value.contains(other = searchText, ignoreCase = true)
              } == true
            }
          }

        onReceiveData(geoJsonFeatures)

        Timber.w(
          locationsWithoutCoordinates.joinToString("\n") {
            val position = (it.resource as Location).position
            "Location id ${it.resource.logicalId} coordinates (${position.longitude},${position.latitude}) invalid."
          },
        )
        pageNumber++
        count += DefaultRepository.DEFAULT_BATCH_SIZE
        registerDataCount += geoJsonFeatures.size
        locationsWithoutCoordinatesCount += locationsWithoutCoordinates.size
      }

      val locationsCount = if (searchText.isNullOrBlank()) totalCount else registerDataCount

      // Account for locations without coordinates
      if (locationsWithoutCoordinatesCount in 1..locationsCount) {
        val message =
          context.getString(
            R.string.locations_without_coordinates,
            locationsWithoutCoordinatesCount,
            locationsCount,
          )
        Timber.w(message)
        emitSnackBarState(
          SnackBarMessageConfig(
            message = message,
            actionLabel = context.getString(org.smartregister.fhircore.engine.R.string.ok),
            duration = SnackbarDuration.Long,
          ),
        )
      } else {
        val message =
          if (searchText.isNullOrBlank()) {
            context.getString(R.string.all_locations_rendered)
          } else context.getString(R.string.all_matching_locations_rendered, locationsCount)
        emitSnackBarState(
          SnackBarMessageConfig(
            message = message,
            actionLabel = context.getString(org.smartregister.fhircore.engine.R.string.ok),
            duration = SnackbarDuration.Short,
          ),
        )
      }

      // Account for missing locations
      if (locationsCount == 0L) {
        if (!searchText.isNullOrBlank()) {
          val message =
            context.getString(
              R.string.no_found_locations_matching_text,
              searchText,
            )
          Timber.w(message)
          emitSnackBarState(
            SnackBarMessageConfig(
              message = message,
              actionLabel = context.getString(org.smartregister.fhircore.engine.R.string.ok),
              duration = SnackbarDuration.Long,
            ),
          )
        } else {
          SnackBarMessageConfig(
            message = context.getString(R.string.no_locations_to_render),
            actionLabel = context.getString(org.smartregister.fhircore.engine.R.string.ok),
            duration = SnackbarDuration.Long,
          )
        }
      }
    }
  }

  fun onEvent(geoWidgetEvent: GeoWidgetEvent) {
    when (geoWidgetEvent) {
      is GeoWidgetEvent.SearchFeatures ->
        retrieveLocations(
          geoWidgetEvent.geoWidgetConfig,
          geoWidgetEvent.searchQuery.query,
          geoWidgetEvent.onReceiveData,
        )
    }
  }

  suspend fun showNoLocationDialog(geoWidgetConfiguration: GeoWidgetConfiguration) {
    geoWidgetConfiguration.noResults?.let {
      _noLocationFoundDialog.postValue(
        context.retrieveRelatedEntitySyncLocationState(MultiSelectViewAction.SYNC_DATA).isEmpty(),
      )
    }
  }

  suspend fun onQuestionnaireSubmission(
    extractedResourceIds: List<IdType>,
    handleGeoJsonFeature: (List<GeoJsonFeature>) -> Unit,
  ) {
    val locationId =
      extractedResourceIds.firstOrNull { it.resourceType == ResourceType.Location.name } ?: return
    val location =
      defaultRepository.loadResource<Location>(locationId.valueAsString.extractLogicalIdUuid())
        ?: return

    val geoJsonFeature =
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
    handleGeoJsonFeature(listOf(geoJsonFeature))
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

  fun getImageBitmap(reference: String) = runBlocking {
    reference.referenceToBitmap(defaultRepository.fhirEngine, decodedImageMap)
  }

  private companion object {
    const val KEY_LATITUDE = "positionLatitude"
    const val KEY_LONGITUDE = "positionLongitude"
  }
}
