package org.smartregister.fhircore.geowidget.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.DatabaseErrorStrategy
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.NetworkConfiguration
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.Authenticator
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.geowidget.BuildConfig
import org.smartregister.fhircore.geowidget.KujakuConversionInterface

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-08-2022. */
class GeowidgetViewModel : ViewModel() {

  val fhirEngine: FhirEngine by lazy {
    FhirEngineProvider.getInstance(context)
  }
  lateinit var context: Context

  suspend fun getFamiliesFeatureCollection(): FeatureCollection {
    val families = getFamilies()

    val featureCollection =
      KujakuConversionInterface()
        .generateFeatureCollection(context, families.map { listOf(it.first, it.second) })

    return featureCollection
  }

  fun getFamiliesFeatureCollectionStream(): LiveData<FeatureCollection> {
    val featureCollectionLiveData = MutableLiveData<FeatureCollection>()

    viewModelScope.launch(Dispatchers.IO) {
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
      fhirEngine.search<Group> { filter(Group.TYPE, { value = of(coding) }) }.filter {
        // it.hasExtension("http://build.fhir.org/extension-location-boundary-geojson.html")
        it.characteristic.firstOrNull { characteristic ->
          characteristic.value is Reference &&
            characteristic.valueReference.reference.contains("Location")
        } != null
      }

    val familiesList = ArrayList<Pair<Group, Location>>()

    familiesWithLocations.forEach { family ->
      val familyLocation =
        fhirEngine.get<Location>(
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

  fun saveLocation(location: Location) : LiveData<Boolean> {
    val liveData = MutableLiveData<Boolean>()
    viewModelScope.launch (Dispatchers.IO) {
      fhirEngine.create(location)
      liveData.postValue(true)
    }

    return liveData
  }
/*
  fun getFhirEngine(): FhirEngine {
    if (!this::fhirEngine1.isInitialized) {
*//*
      FhirEngineProvider.init(
        FhirEngineConfiguration(
          enableEncryptionIfSupported = !BuildConfig.DEBUG,
          DatabaseErrorStrategy.UNSPECIFIED,
          ServerConfiguration(
            baseUrl = "https://fhir.labs.smartreigster.org/fhir/",
            authenticator =
              object : Authenticator {
                override fun getAccessToken() = ""
              },
            networkConfiguration = NetworkConfiguration(120, 120, 120)
          )
        )
      )*//*

      fhirEngine1 = FhirEngineProvider.getInstance(context)
    }

    return fhirEngine1
  }*/
}
