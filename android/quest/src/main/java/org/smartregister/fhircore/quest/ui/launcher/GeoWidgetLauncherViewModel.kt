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
import android.text.format.DateUtils.isToday
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.model.Position
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

const val PAGE_SIZE = 20

@HiltViewModel
class GeoWidgetLauncherViewModel
@Inject
constructor(
    val defaultRepository: DefaultRepository,
    val dispatcherProvider: DispatcherProvider,
    val sharedPreferencesHelper: SharedPreferencesHelper,
) : ViewModel() {

    private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
    val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

    private val _locationsFlow: MutableStateFlow<Set<GeoWidgetLocation>> =
        MutableStateFlow(setOf())
    val locationsFlow: StateFlow<Set<GeoWidgetLocation>> = _locationsFlow

    private val _locationDialog = MutableLiveData<String>()
    val locationDialog: LiveData<String> get() = _locationDialog

    // TODO: use List or Linkage resource to connect Location with Group/Patient/etc
    private fun retrieveLocations() {
        viewModelScope.launch(dispatcherProvider.io()) {
            val totalResource = defaultRepository.count(Search(ResourceType.Location))

            val totalIteration = ceil(totalResource / PAGE_SIZE.toDouble()).toInt()

            repeat(totalIteration) { index ->
                val startingIndex = index * PAGE_SIZE
                val search = Search(ResourceType.Location, PAGE_SIZE, startingIndex)

                defaultRepository.search<Location>(search).forEach { location ->
                    if (location.hasPosition() && location.position.hasLatitude() && location.position.hasLongitude()) {
                        val searchRelatedResources =
                            Search(ResourceType.Encounter).apply {
                                val reference : ReferenceParamFilterCriterion.() -> Unit = {
                                    value = location.logicalId.asReference(location.resourceType).reference
                                }
                                val filters = arrayListOf(reference)
                                filter(
                                    ReferenceClientParam("location"),
                                    *filters.toTypedArray(),
                                )
                            }
                        var visitStatus = "not_started"
                        defaultRepository.search<Encounter>(searchRelatedResources).forEach { encounter ->
                            if (encounter.type[0].coding[0].code == "SVISIT" && isToday(encounter.period.start.time)) {
                                visitStatus = encounter.status.display.replace(" ", "_").lowercase()
                            }
                        }
                        val geoWidgetLocation = GeoWidgetLocation(
                            id = location.id,
                            name = location.name ?: "",
                            position = Position(
                                location.position.latitude.toDouble(),
                                location.position.longitude.toDouble()
                            ),
                            status = location.status.name,
                            type = location.type.find { codeableConcept ->
                                codeableConcept.coding[0].system == "http://terminology.hl7.org/CodeSystem/v3-RoleCode" && codeableConcept.coding[0].code != "work"
                            }?.coding?.get(0)?.code ?: "",
                            typeText = location.type.find { codeableConcept ->
                                codeableConcept.coding[0].system == "http://terminology.hl7.org/CodeSystem/v3-RoleCode" && codeableConcept.coding[0].code != "work"
                            }?.coding?.get(0)?.display ?: "",
                            parentLocationId = location.partOf.reference,
                            // TODO: add logic to decide the color of location
                            visitStatus = visitStatus
                        )
                        addLocationToFlow(geoWidgetLocation)
                    }
                }
            }
        }
    }

    fun checkSelectedLocation() {
        //check preference if location/region is already selected otherwise show dialog to select location
        //through Location Selector Feature/Screen
        //todo - for now we are calling this method, once location Selector is developed, we can remove this line
        retrieveLocations()
    }

    private fun addLocationToFlow(location: GeoWidgetLocation) {
        Timber.i("Location position lat: ${location.position?.latitude} and long: ${location.position?.longitude}")
        _locationsFlow.value = _locationsFlow.value + location
    }

    private suspend fun getLocationFromDb(id: String): Location? {
        return defaultRepository.loadResource(id.extractLogicalIdUuid())
    }

    suspend fun onQuestionnaireSubmission(extractedResourceIds: List<IdType>) {
        val locationId =
            extractedResourceIds.firstOrNull { it.resourceType == ResourceType.Location.name }
                ?: return
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

    fun launchQuestionnaire(
        questionnaireConfig: QuestionnaireConfig,
        geoWidgetLocation: GeoWidgetLocation,
        context: Context,
    ) {
        val params = addMatchingCoordinatesToActionParameters(
            geoWidgetLocation.position?.latitude,
            geoWidgetLocation.position?.longitude,
            questionnaireConfig.extraParams
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
                //TODO: here the search bar query will be processed
                ""
            }
        }

    /** Adds coordinates into the correct action parameter as [ActionParameter.value] if the [ActionParameter.key] matches with [KEY_LATITUDE] or [KEY_LONGITUDE] constants. **/
    private fun addMatchingCoordinatesToActionParameters(
        latitude: Double?,
        longitude: Double?,
        params: List<ActionParameter>?
    ): List<ActionParameter> {
        if (latitude == null || longitude == null) {
            throw IllegalArgumentException("Latitude or Longitude must not be null")
        }
        params ?: return emptyList()
        return params
            .filter {
                it.paramType == ActionParameterType.PREPOPULATE && it.dataType == Enumerations.DataType.STRING
            }.map {
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
