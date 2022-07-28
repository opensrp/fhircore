/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.extension.interpolate

@Singleton
class LocaleUtil @Inject constructor(val configurationRegistry: ConfigurationRegistry) {
  companion object {
    const val STRINGS_BASE_BUNDLE_NAME = "strings"
  }
  /**
   * @param bundleName
   * @param locale
   * @param template
   *
   * @return String of the interpolated template string
   */
  fun parseTemplate(bundleName: String, locale: Locale, template: String): String {
    return try {
      val bundle =
        configurationRegistry.retrieveResourceBundleConfiguration(bundleName(bundleName, locale))
          ?: ResourceBundle.getBundle(bundleName, locale)
      val lookup = mutableMapOf<String, Any>()
      bundle.keys.toList().forEach { lookup[it] = bundle.getObject(it) }
      template.interpolate(lookup, "{{", "}}")
    } catch (exception: MissingResourceException) {
      template
    }
  }
  // To Discuss whether we need the APP_ID key here i.e. whether we need to distinguish by specific
  // app
  // fun bundleName(baseBundle:String, locale: Locale) =
  // "${configurationRegistry.sharedPreferencesHelper.read(APP_ID_KEY,
  // "")?.trimEnd()}_${baseBundle}_${locale.toLanguageTag()}"

  /**
   * @param baseBundle base name of the bundle e.g. strings
   * @param locale the specific locale. The tag returned for French locale is fr - The resolved
   * bundle name will be strings_fr
   */
  fun bundleName(baseBundle: String, locale: Locale) = "${baseBundle}_${locale.toLanguageTag()}"

  /**
   * Creates identifier from text by doing clean up on the passed value
   *
   * @param text value to be translated
   * @return string.properties key to be used in string look ups
   */
  fun generateIdentifier(text: String): String? {
    val prefix = if (text.matches(Regex("^\\d.*\\n*"))) "_" else ""
    return prefix.plus(
      text.trim { it <= ' ' }.lowercase(Locale.ENGLISH).replace(" ".toRegex(), "_")
    )
  }

  fun formatMessage(locale: Locale?, pattern: String?, vararg arguments: Any?) =
    MessageFormat(pattern, locale).format(arguments)
}
