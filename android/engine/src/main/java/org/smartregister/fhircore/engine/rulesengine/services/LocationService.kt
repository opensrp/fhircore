package org.smartregister.fhircore.engine.rulesengine.services

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.location.LocationUtils.Companion.getAccurateLocation

object LocationService {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var retrievedGPSLocation: Location? = null

    fun init(fusedLocationProviderClient: FusedLocationProviderClient) {
        fusedLocationClient = fusedLocationProviderClient
    }

    fun calculateDistanceByProvidedLocations(destination: Location, currentLocation: Location): String? {
        val distanceInMeters = currentLocation.distanceTo(destination)
        return formatDistance(distanceInMeters)
    }

    fun calculateDistanceByGpsLocation(location: Resource): String? {
        val currentLocation = generateLocation(location)
        CoroutineScope(Dispatchers.IO).launch {
            retrievedGPSLocation = getAccurateLocation(fusedLocationClient)
        }
        val distanceInMeters =  retrievedGPSLocation?.let { calculateDistance(currentLocation!!, it) }
        return distanceInMeters?.let { formatDistance(it) }
    }

    private fun generateLocation(location: Resource): Location? {
        return (location as? org.hl7.fhir.r4.model.Location)?.let {
            Location("CustomLocationProvider").apply {
                longitude = it.position.longitude.toDouble()
                latitude = it.position.latitude.toDouble()
            }
        }
    }

    private fun calculateDistance(locationA: Location, locationB: Location): Float {
        val resultArray = FloatArray(1)
        Location.distanceBetween(
            locationA.latitude,
            locationA.longitude,
            locationB.latitude,
            locationB.longitude,
            resultArray
        )
        return resultArray[0]
    }

    private fun formatDistance(distanceInMeters: Float): String {
        return if (distanceInMeters < 1000) {
            String.format("%.2f mtrs", distanceInMeters)
        } else {
            val distanceInKilometers = distanceInMeters / 1000.0
            String.format("%.2f km", distanceInKilometers)
        }
    }

}