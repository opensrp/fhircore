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

package org.smartregister.fhircore.quest.ui.launcher

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.filter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.geowidget.model.Coordinates
import org.smartregister.fhircore.geowidget.model.Feature
import org.smartregister.fhircore.geowidget.model.Geometry
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import timber.log.Timber

@HiltViewModel
class GeoWidgetLauncherViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
) : ViewModel() {

  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

  private val _locationsFlow: MutableStateFlow<Set<Feature>> = MutableStateFlow(setOf())
  val locationsFlow: StateFlow<Set<Feature>> = _locationsFlow

  private val _shouldTriggerSearchQuery: MutableStateFlow<Int> = MutableStateFlow(0)
  val shouldTriggerSearchQuery: StateFlow<Int> = _shouldTriggerSearchQuery

  private val _locationDialog = MutableLiveData<String>()
  val locationDialog: LiveData<String>
    get() = _locationDialog

  val searchText = mutableStateOf("")
  private var geoWidgetConfiguration: GeoWidgetConfiguration? = null

  // TODO: use List or Linkage resource to connect Location with Group/Patient/etc
  private fun retrieveLocations(geoWidgetConfig: GeoWidgetConfiguration) {
    viewModelScope.launch(dispatcherProvider.io()) {
      // TODO: Loading all the data with the related resources may impact performance. This
      //  needs to be refactored in future
      val repositoryResourceDataList =
        defaultRepository.searchResourcesRecursively(
          filterActiveResources = null,
          fhirResourceConfig = geoWidgetConfig.resourceConfig,
          configRules = null,
          secondaryResourceConfigs = null,
          filterByRelatedEntityLocationMetaTag = false,
        )

      repositoryResourceDataList.forEach { repositoryResourceData ->
        val location = repositoryResourceData.resource as Location
        val resourceData =
          resourceDataRulesExecutor.processResourceData(
            repositoryResourceData = repositoryResourceData,
            ruleConfigs = geoWidgetConfig.servicePointConfig?.rules!!,
            params = emptyMap(),
          )
        val servicePointProperties = mutableMapOf<String, Any>()
        geoWidgetConfig.servicePointConfig?.servicePointProperties?.forEach { (key, value) ->
          servicePointProperties[key] = value.interpolate(resourceData.computedValuesMap)
        }
        if (
          location.hasPosition() &&
            location.position.hasLatitude() &&
            location.position.hasLongitude()
        ) {
          val feature =
            Feature(
              id = location.idElement.idPart,
              geometry =
                Geometry(
                  coordinates =
                    arrayListOf(
                      Coordinates(
                        latitude = location.position.latitude.toDouble(),
                        longitude = location.position.longitude.toDouble(),
                      ),
                    ),
                ),
              properties = servicePointProperties,
            )
          addLocationToFlow(feature)
          Timber.i("GeoWidgetLauncherViewModel:addLocationToMap")
        }
      }
      //once data is loaded to map, its time to pass the searchQuery to get invoked
      if (repositoryResourceDataList.isNotEmpty()) {
        Timber.i("GeoWidgetLauncherViewModel:searchQuery")
        _shouldTriggerSearchQuery.value++
      }
    }
  }

  fun checkSelectedLocation(configuration: GeoWidgetConfiguration) {
    // check preference if location/region is already selected otherwise show dialog to select
    // location
    // through Location Selector Feature/Screen
    // todo - for now we are calling this method, once location Selector is developed, we can remove
    // this line
    this.geoWidgetConfiguration = configuration
    retrieveLocations(configuration)
  }

  private fun addLocationToFlow(location: Feature) {
    _locationsFlow.value = _locationsFlow.value + location
  }

  private suspend fun getLocationFromDb(id: String): Location? {
    return defaultRepository.loadResource(id.extractLogicalIdUuid())
  }

  suspend fun onQuestionnaireSubmission(extractedResourceIds: List<IdType>) {
    val locationId =
      extractedResourceIds.firstOrNull { it.resourceType == ResourceType.Location.name } ?: return
    val location = getLocationFromDb(locationId.valueAsString) ?: return

    val feature =
      Feature(
        id = location.id,
        geometry =
          Geometry(
            coordinates =
              listOf(
                Coordinates(
                  latitude = location.position.latitude.toDouble(),
                  longitude = location.position.longitude.toDouble(),
                ),
              ),
          ),
        // TODO: add initial color for location
      )
    addLocationToFlow(feature)
  }

  fun launchQuestionnaire(
    questionnaireConfig: QuestionnaireConfig,
    feature: Feature,
    context: Context,
  ) {
    val params =
      addMatchingCoordinatesToActionParameters(
        feature.geometry?.coordinates?.get(0)?.latitude!!,
        feature.geometry?.coordinates?.get(0)?.longitude,
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

  fun onEvent(event: GeoWidgetEvent) =
    when (event) {
      is GeoWidgetEvent.SearchServicePoints -> {
        searchText.value = event.searchText
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

  private companion object {
    const val KEY_LATITUDE = "positionLatitude"
    const val KEY_LONGITUDE = "positionLongitude"
  }
}
