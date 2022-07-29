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

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.extension.interpolate

class LocaleUtil(val configurationRegistry: ConfigurationRegistry) {
  /**
   * @param bundleName base name of the bundle e.g. strings
   * @param locale the specific Locale, e.g. Locale.FRENCH. The language tag returned for French
   * locale is fr
   * @param template the input string with placeholder variables to be replaced with translated
   * values
   *
   * @return String of the interpolated template string
   */
  fun parseTemplate(bundleName: String, locale: Locale, template: String): String {
    return try {
      val bundle =
        configurationRegistry.retrieveResourceBundleConfiguration(
          getLocaleSpecificFileName(bundleName, locale)
        )
          ?: ResourceBundle.getBundle(bundleName, locale)
      val lookup = mutableMapOf<String, Any>()
      bundle.keys.toList().forEach { lookup[it] = bundle.getObject(it) }
      template.interpolate(lookup, "{{", "}}")
    } catch (exception: MissingResourceException) {
      template
    }
  }

  /**
   * @param baseBundle base name of the bundle e.g. strings
   * @param locale the specific locale. The tag returned for French locale is fr
   *
   * @return The locale specific filename, in this case will be: strings_fr
   */
  fun getLocaleSpecificFileName(baseBundle: String, locale: Locale) =
    "${baseBundle}_${locale.toLanguageTag()}"

  /**
   * Creates identifier from text by doing clean up on the passed value
   *
   * @param text value to be translated
   * @return string.properties key to be used in string look ups
   */
  fun generateIdentifier(text: String): String? {
    return text.trim { it <= ' ' }.lowercase(Locale.ENGLISH).replace(" ".toRegex(), ".")
  }

  companion object {
    const val STRINGS_BASE_BUNDLE_NAME = "strings"
  }
}
