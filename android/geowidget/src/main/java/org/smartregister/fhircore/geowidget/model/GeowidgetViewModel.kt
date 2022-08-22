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

package org.smartregister.fhircore.geowidget.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import com.mapbox.geojson.FeatureCollection
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.geowidget.KujakuFhirCoreConverter

class GeowidgetViewModel
@Inject
constructor(val defaultRepository: DefaultRepository, val dispatcherProvider: DispatcherProvider) :
  ViewModel() {

  lateinit var context: Context

  suspend fun getFamiliesFeatureCollection(): FeatureCollection {
    val families = getFamilies()

    return KujakuFhirCoreConverter()
      .generateFeatureCollection(context, families.map { listOf(it.first, it.second) })
  }

  fun getFamiliesFeatureCollectionStream(): LiveData<FeatureCollection> {
    val featureCollectionLiveData = MutableLiveData<FeatureCollection>()

    viewModelScope.launch(dispatcherProvider.io()) {
      val familyFeatures = getFamiliesFeatureCollection()
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
        .filter {
          // it.hasExtension("http://build.fhir.org/extension-location-boundary-geojson.html")
          it.characteristic.firstOrNull { characteristic ->
            characteristic.value is Reference &&
              characteristic.valueReference.reference.contains("Location")
          } != null
        }

    val familiesList = ArrayList<Pair<Group, Location>>()

    familiesWithLocations.forEach { family ->
      val familyLocation =
        defaultRepository.fhirEngine.get<Location>(
          family.characteristic.firstOrNull { characteristic ->
              characteristic.value is Reference &&
                characteristic.valueReference.reference.contains("Location")
            }!!
            .valueReference
            .referenceElement
            .idPart
        )

      familiesList.add(Pair(family, familyLocation))
    }

    return familiesList
  }

  fun saveLocation(location: Location): LiveData<Boolean> {
    val liveData = MutableLiveData<Boolean>()
    viewModelScope.launch(dispatcherProvider.io()) {
      defaultRepository.save(location)
      liveData.postValue(true)
    }

    return liveData
  }
}
