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

package org.smartregister.fhircore.engine.util

import java.util.Locale
import org.hl7.fhir.r4.model.Basic
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import timber.log.Timber

/**
 * Utility class for creating and managing a FHIR Basic resource that represents the current device
 * language. This resource is used as a launch context in questionnaires, allowing variable
 * definitions to conditionally display text based on the device's language setting.
 *
 * The Basic resource contains language information using ISO 639-1 language codes (e.g., en, es,
 * fr).
 */
object LanguageBasicUtil {
  /**
   * Fixed identifier for the device language resource. This ensures the resource can be
   * consistently referenced and updated.
   */
  const val LANGUAGE_BASIC_ID = "device-language"

  /**
   * Creates a Basic FHIR resource representing the current device language.
   *
   * @return A Basic FHIR resource configured with the current device language information.
   *
   * Example usage:
   * ```kotlin
   * val languageBasic = LanguageBasicUtil.createLanguageBasic()
   * // languageBasic can then be used as a launch context in questionnaires
   * ```
   */
  fun createLanguageBasic(): Basic {
    val languageCode = getCurrentLanguageCode()
    val languageDisplay = getCurrentLanguageDisplay()

    return Basic().apply {
      id = LANGUAGE_BASIC_ID

      code =
        CodeableConcept()
          .addCoding(
            Coding(
              "urn:ietf:bcp:47",
              languageCode,
              languageDisplay,
            ),
          )

      Timber.d("Created language basic resource with language code: $languageCode")
    }
  }

  /**
   * Retrieves the current device language code in ISO 639-1 format (e.g., en, es, fr).
   *
   * @return The ISO 639-1 language code of the device's current locale.
   */
  private fun getCurrentLanguageCode(): String {
    return Locale.getDefault().language
  }

  /**
   * Retrieves the display name for the current device language.
   *
   * @return The display name of the device's language (e.g., "English", "Spanish", "French").
   */
  private fun getCurrentLanguageDisplay(): String {
    return Locale.getDefault().displayLanguage
  }

  /**
   * Checks if a language code matches the current device language.
   *
   * This is useful for FHIRPath expressions that need to conditionally evaluate based on language.
   *
   * @param languageCode The ISO 639-1 language code to check (e.g., "en", "es", "fr").
   * @return True if the device language matches the provided code, false otherwise.
   */
  fun isLanguage(languageCode: String): Boolean {
    return getCurrentLanguageCode() == languageCode
  }
}
