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

package org.smartregister.fhircore.geowidget.screens

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.util.extensions.getGeoJsonGeometry

@HiltViewModel
class GeoWidgetViewModel @Inject constructor(val dispatcherProvider: DispatcherProvider) :
  ViewModel() {

  private val _featuresFlow: MutableStateFlow<Set<Feature>> =
    MutableStateFlow(setOf())
  val featuresFlow: StateFlow<Set<Feature>> = _featuresFlow

    return KujakuFhirCoreConverter()
      .generateFeatureCollection(context, families.map { listOf(it.first, it.second) })
  }

  fun getFamiliesFeatureCollectionStream(context: Context): LiveData<FeatureCollection> {
    val featureCollectionLiveData = MutableLiveData<FeatureCollection>()

    viewModelScope.launch(dispatcherProvider.io()) {
      val familyFeatures = getFamiliesFeatureCollection(context)
      featureCollectionLiveData.postValue(familyFeatures)
    }

    return featureCollectionLiveData
  }

  suspend fun getFamilies(): List<Pair<Group, Location>> {
    val coding =
      Coding().apply {
        system = "http://hl7.org/fhir/group-type"
        code = "person"
      }

    val familiesWithLocations =
      defaultRepository.fhirEngine
        .search<Group> { filter(Group.TYPE, { value = of(coding) }) }
        .asSequence()
        .map { it.resource }
        .filter {
          // it.hasExtension("http://build.fhir.org/extension-location-boundary-geojson.html")
          it.characteristic.firstOrNull { characteristic ->
            characteristic.value is Reference &&
              characteristic.valueReference.reference.contains(ResourceType.Location.name)
          } != null
        }

    val familiesList = ArrayList<Pair<Group, Location>>()

    familiesWithLocations.forEach { family ->
      try {
        val familyLocation = defaultRepository.fhirEngine.get<Location>(familyLocationId(family))

        familiesList.add(Pair(family, familyLocation))
      } catch (ex: ResourceNotFoundException) {
        Timber.e(ex)
      }
    }

    return familiesList
  }

  private fun familyLocationId(family: Group) =
    family.characteristic
      .firstOrNull { characteristic ->
        characteristic.value is Reference &&
          characteristic.valueReference.reference.contains("Location")
      }!!
      .valueReference
      .referenceElement
      .idPart

  fun saveLocation(location: Location): LiveData<Boolean> {
    val liveData = MutableLiveData<Boolean>()
    viewModelScope.launch(dispatcherProvider.io()) {
      defaultRepository.create(true, location)
      liveData.postValue(true)
    }

    return liveData
  }
}
