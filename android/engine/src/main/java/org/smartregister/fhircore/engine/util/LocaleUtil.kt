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
import org.apache.commons.text.StringSubstitutor

object LocaleUtil {

  fun parseTemplate(bundleName: String, locale: Locale, template: String): String {
    return try {
      val bundle = ResourceBundle.getBundle(bundleName, locale)
      getBundleStringSubstitutor(bundle).replace(template)
    } catch (exception: MissingResourceException) {
      template
    }
  }

  @Throws(IllegalArgumentException::class)
  private fun getBundleStringSubstitutor(resourceBundle: ResourceBundle): StringSubstitutor {
    val lookup = mutableMapOf<String, Any>()
    resourceBundle.keys.toList().forEach { lookup[it] = resourceBundle.getObject(it) }
    return StringSubstitutor(lookup, "{{", "}}")
  }

  // TO DO prefix with appId
  // fun String.localize(): String = parseTemplate(appId + "strings", Locale.getDefault(), this)
  fun String.localize(): String = parseTemplate("strings", Locale.getDefault(), this)

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
}
