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

package org.smartregister.fhircore.quest.util

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.apache.commons.text.StringSubstitutor

object LocaleUtil {
  @Throws(IllegalArgumentException::class)
  private fun getBundleStringSubstitutor(resourceBundle: ResourceBundle): StringSubstitutor {
    val lookup = mutableMapOf<String, Any>()
    resourceBundle.keys.toList().forEach { lookup[it] = resourceBundle.getObject(it) }
    return StringSubstitutor(lookup, "{{", "}}")
  }

  fun parseTemplate(bundleName: String, locale: Locale, template: String): String {
    return try {
      val bundle = ResourceBundle.getBundle(bundleName, locale)
      getBundleStringSubstitutor(bundle).replace(template.replace("\\s+".toRegex(), ""))
    } catch (exception: MissingResourceException) {
      template
    }
  }

  fun getBundleNameFromFileSource(fileSource: String) =
    fileSource.run { substring(lastIndexOf('/') + 1, lastIndexOf('.')) }
}
