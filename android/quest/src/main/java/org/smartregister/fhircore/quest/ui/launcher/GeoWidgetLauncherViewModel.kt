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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.search.Search
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.IdType
import javax.inject.Inject
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.model.Position
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import timber.log.Timber
import kotlin.math.ceil

const val PAGE_SIZE = 20

@HiltViewModel
class GeoWidgetLauncherViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _locationsFlow: MutableStateFlow<Set<GeoWidgetLocation>> =
        MutableStateFlow(setOf())
    val locationsFlow: StateFlow<Set<GeoWidgetLocation>> = _locationsFlow

    // TODO: use List or Linkage resource to connect Location with Group/Patient/etc
    fun retrieveLocations() {
      viewModelScope.launch(dispatcherProvider.io()) {
          val totalResource = defaultRepository.count(Search(ResourceType.Location))

          val totalIteration = ceil(totalResource / PAGE_SIZE.toDouble()).toInt()

          repeat(totalIteration) { index ->
              val startingIndex = index * PAGE_SIZE
              val search = Search(ResourceType.Location, PAGE_SIZE, startingIndex)

              defaultRepository.search<Location>(search).forEach { location ->
                  if (location.hasPosition() && location.position.hasLatitude() && location.position.hasLongitude()) {
                      val geoWidgetLocation = GeoWidgetLocation(
                          id = location.id,
                          name = location.name ?: "",
                          position = Position(
                              location.position.latitude.toDouble(),
                              location.position.longitude.toDouble()
                          ),
                          // TODO: add logic to decide the color of location
                      )
                      addLocationToFlow(geoWidgetLocation)
                  }
              }
          }
      }
  }

    private fun addLocationToFlow(location: GeoWidgetLocation) {
        Timber.i("Location position lat: ${location.position?.latitude} and long: ${location.position?.longitude}")
        _locationsFlow.value = _locationsFlow.value + location
    }

    private suspend fun getLocationFromDb(id: String): Location? {
        return defaultRepository.loadResource(id.extractLogicalIdUuid())
    }

    suspend fun onQuestionnaireSubmission(extractedResourceIds: List<IdType>) {
        val locationId = extractedResourceIds.firstOrNull { it.resourceType == ResourceType.Location.name } ?: return
        val location = getLocationFromDb(locationId.valueAsString) ?: return

        val geoWidgetLocation = GeoWidgetLocation(
            id = location.id,
            name = location.name,
            position = Position(
                latitude = location.position.latitude.toDouble(),
                longitude = location.position.longitude.toDouble(),
            ),
            // TODO: add initial color for location
        )
        addLocationToFlow(geoWidgetLocation)
    }

    fun launchQuestionnaireWithParams(
        geoWidgetLocation: GeoWidgetLocation,
        context: Context,
        questionnaireConfig: QuestionnaireConfig,
    ) {
        val latitudeParam =
            ActionParameter(
                key = "locationLatitude",
                paramType = ActionParameterType.PREPOPULATE,
                dataType = Enumerations.DataType.STRING,
                resourceType = ResourceType.Location,
                value = geoWidgetLocation.position?.latitude.toString(),
                linkId = "location-latitude",
            )
        val longitudeParam =
            ActionParameter(
                key = "locationLongitude",
                paramType = ActionParameterType.PREPOPULATE,
                dataType = Enumerations.DataType.STRING,
                resourceType = ResourceType.Location,
                value = geoWidgetLocation.position?.longitude.toString(),
                linkId = "location-longitude",
            )
        if (context is QuestionnaireHandler) {
            context.launchQuestionnaire(
                context = context,
                questionnaireConfig = questionnaireConfig,
                actionParams = listOf(latitudeParam, longitudeParam),
            )
        }
    }
}
