package org.smartregister.fhircore.engine.app.di.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.smartregister.fhircore.engine.app.testDispatcher
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import org.smartregister.fhircore.engine.di.DataStoreModule
import javax.inject.Singleton


@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
class FakeDataStoreModule {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Provides
  @Singleton
  fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
    val testCoroutineScope = CoroutineScope(testDispatcher + SupervisorJob())
    val testDataStoreName = "test_datastore"
    return PreferenceDataStoreFactory.create(
        scope = testCoroutineScope,
        produceFile = {
          context.preferencesDataStoreFile(testDataStoreName)
        },
      )
  }

  @Provides
  @Singleton
  fun providePreferencesDataStore(@ApplicationContext context: Context, testDataStore: DataStore<Preferences>): PreferencesDataStore =
    PreferencesDataStore(
      context,
      testDataStore
    )
}