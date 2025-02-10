/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import java.util.Locale
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages

class LanguageSelector
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
) {
  fun getDefaultLocale(baseContext: Context): String {
    val sharedPrefLocale =
      baseContext
        .getSharedPreferences(SharedPreferencesHelper.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(SharedPreferenceKey.LANG.name, null)
    val deviceDefaultLocale = Locale.getDefault()
    val applicationConfiguration =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    val appConfigDefaultLocale = applicationConfiguration.defaultLocale
    val firstLocaleInLanguagesList = applicationConfiguration.languages.first()

    return when {
      !sharedPrefLocale.isNullOrBlank() && isLanguageSupported(sharedPrefLocale) -> sharedPrefLocale
      isLanguageSupported(deviceDefaultLocale.toLanguageTag()) ->
        deviceDefaultLocale.toLanguageTag()
      appConfigDefaultLocale.isNotBlank() && isLanguageSupported(appConfigDefaultLocale) ->
        appConfigDefaultLocale
      else -> firstLocaleInLanguagesList
    }
  }

  private fun isLanguageSupported(lang: String): Boolean {
    return configurationRegistry.fetchLanguages().any { it.equals(lang) }
  }
}
