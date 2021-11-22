package org.smartregister.fhircore.engine.di

import android.accounts.AccountManager
import android.content.Context
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.sync.Sync
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

@InstallIn(SingletonComponent::class)
@Module(includes = [NetworkModule::class, DispatcherModule::class])
class EngineModule {

  @Singleton
  @Provides
  fun provideFhirEngine(@ApplicationContext context: Context) =
    FhirEngineProvider.getInstance(context)

  @Singleton
  @Provides
  fun provideSyncJob(@ApplicationContext context: Context) = Sync.basicSyncJob(context)

  @Singleton @Provides fun provideSyncBroadcaster() = SyncBroadcaster

  @Singleton @Provides fun provideWorkerContextProvider() = SimpleWorkerContext()

  @Singleton
  @Provides
  fun provideApplicationManager(@ApplicationContext context: Context): AccountManager =
    AccountManager.get(context)
}
