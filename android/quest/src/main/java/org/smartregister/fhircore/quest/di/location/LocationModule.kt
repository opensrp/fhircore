package org.smartregister.fhircore.quest.di.location

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.quest.location.LocationClient
import org.smartregister.fhircore.quest.location.LocationClientImpl
import org.smartregister.fhircore.quest.location.LocationService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Singleton
    @Provides
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)


    @Singleton
    @Provides
    fun provideLocationClient(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationClient = LocationClientImpl(context, fusedLocationProviderClient)

    @Singleton
    @Provides
    fun provideService() = LocationService()
}

