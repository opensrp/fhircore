package org.smartregister.fhircore.quest.location

import android.app.Service
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationServices
import android.os.IBinder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationClientProvider{
    val locationClient: LocationClient
}


@AndroidEntryPoint
class LocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() )
//    @Inject
//    lateinit var locationClient: LocationClient

//    @ApplicationContext context: Contex
    val locationClient = EntryPointAccessors.fromApplication(applicationContext, LocationClientProvider::class.java).locationClient

    //var loc:Location? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

//    override fun onCreate() {
//        super.onCreate()
//
//        locationClient = LocationClientImpl(
//            applicationContext,
//            LocationServices.getFusedLocationProviderClient(applicationContext)
//        )
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun start(): Location? {
        var loc: Location? = null
        locationClient
            .getLocationUpdate(10000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                loc = location
                Timber.d("Service Location: ${location.latitude},${location.longitude}")
            }
            .launchIn(serviceScope)
        return loc
    }

    private fun stop() {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}