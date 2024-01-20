package org.smartregister.fhircore.engine.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import javax.inject.Singleton
@InstallIn(SingletonComponent::class)
@Module
class DataStoreModule {

  @Singleton
  @Provides
  fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
    val userPreferences = "preferences_datastore"
    return PreferenceDataStoreFactory.create(
      scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(userPreferences) },
    )
  }

  @Singleton
  @Provides
  fun providePreferencesDataStore(@ApplicationContext context: Context, dataStore: DataStore<Preferences>): PreferencesDataStore {
    return PreferencesDataStore(
      context,
      dataStore
    )
  }
}