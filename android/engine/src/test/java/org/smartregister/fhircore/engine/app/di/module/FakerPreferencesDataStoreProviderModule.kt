package org.smartregister.fhircore.engine.app.di.module

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.smartregister.fhircore.engine.app.testDispatcher
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import org.smartregister.fhircore.engine.di.DataStoreModule
import javax.inject.Singleton


@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
class FakerPreferencesDataStoreProviderModule {

  private val testCoroutineScope = CoroutineScope(testDispatcher + Job())
  private val testDataStoreName = "test_datastore"
  val testDataStore =
    PreferenceDataStoreFactory.create(
      scope = testCoroutineScope,
      produceFile = {
        ApplicationProvider.getApplicationContext<Application>().preferencesDataStoreFile(testDataStoreName)
      },
    )

  @Provides
  @Singleton
  fun providePreferencesDataStore(): PreferencesDataStore =
    PreferencesDataStore(
      ApplicationProvider.getApplicationContext<Application>(),
      Gson(),
      testDataStore,
    )
}