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
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireLocation
import timber.log.Timber
import javax.inject.Inject



@AndroidEntryPoint
class LocationService: Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() )

    @Inject lateinit var locationClient:LocationClient
    @Inject
    lateinit var sharedPreferences: SharedPreferencesHelper

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun start() {
        locationClient
            .getLocationUpdate(10000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                // write location to shared pref
                Timber.d("Service Location: ${location.latitude},${location.longitude}")
                writeLocation(
                    QuestionnaireLocation(
                        latitide = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude
                    )
                )
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        stopSelf()
    }

    private fun writeLocation(
        location:QuestionnaireLocation,
    ) {
        sharedPreferences.write(
            key = SharedPreferenceKey.GOOGLE_LOCATION.name,
            value = location,
        )
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