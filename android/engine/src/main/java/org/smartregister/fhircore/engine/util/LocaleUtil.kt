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

import org.apache.commons.text.StringSubstitutor
import java.util.Locale
import java.util.ResourceBundle
import java.util.MissingResourceException


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

    fun String.localize(): String = parseTemplate("strings", Locale.getDefault(), this)
}