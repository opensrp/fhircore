/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Locale
import javax.inject.Inject
import org.smartregister.fhircore.engine.datastore.PreferencesDataStore
import org.smartregister.fhircore.engine.util.extension.setAppLocale

/**
 * Base class for all activities used in the app. Every activity should extend this class for
 * multi-language support.
 */
@AndroidEntryPoint
abstract class BaseMultiLanguageActivity : AppCompatActivity() {

  // TODO: KELVIN discuss with Elly. Lateninit and getSharedPreferences()
  @Inject lateinit var preferencesDataStore: PreferencesDataStore

  override fun onCreate(savedInstanceState: Bundle?) {
    inject()
    super.onCreate(savedInstanceState)

    // Disable dark theme on All Activities.
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
  }

  override fun attachBaseContext(baseContext: Context) {

    val lang =
      preferencesDataStore.readOnce(PreferencesDataStore.LANG, Locale.ENGLISH.toLanguageTag())
    baseContext.setAppLocale(lang!!).run {
      super.attachBaseContext(baseContext)
      applyOverrideConfiguration(this)
    }
  }

  /**
   * This method is required by Hilt to inject dependencies for the base class. Hilt injection
   * occurs within super.onCreate() instead of before super.onCreate()
   */
  protected open fun inject() {
    throw UnsupportedOperationException(
      "Annotate $this with @AndroidEntryPoint annotation. The inject method should be overridden by the Hilt generated class.",
    )
  }
}
