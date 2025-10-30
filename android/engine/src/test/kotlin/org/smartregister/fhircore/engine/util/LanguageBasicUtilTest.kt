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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

/**
 * Unit tests for [LanguageBasicUtil].
 *
 * Tests the creation and management of the Basic FHIR resource representing the device language,
 * and the ability to check language codes.
 */
@RunWith(RobolectricTestRunner::class)
class LanguageBasicUtilTest : RobolectricTest() {

  @Test
  fun testCreateLanguageBasicReturnsValidBasic() {
    val basic = LanguageBasicUtil.createLanguageBasic()

    // Verify the basic resource is created with the correct ID
    Assert.assertEquals(LanguageBasicUtil.LANGUAGE_BASIC_ID, basic.id)

    // Verify the basic resource has a code
    Assert.assertNotNull(basic.code)
    Assert.assertFalse(basic.code.coding.isEmpty())
  }

  @Test
  fun testCreateLanguageBasicHasLanguageCode() {
    val basic = LanguageBasicUtil.createLanguageBasic()

    // Verify the basic resource has a code field with coding
    Assert.assertNotNull(basic.code)
    Assert.assertFalse(basic.code.coding.isEmpty())

    // Verify the coding has the correct system
    val languageCoding = basic.code.coding.first()
    Assert.assertEquals("urn:ietf:bcp:47", languageCoding.system)

    // Verify the language code is in ISO 639-1 format (2-letter code)
    Assert.assertNotNull(languageCoding.code)
    Assert.assertTrue(languageCoding.code.matches(Regex("[a-z]{2}")))

    // Verify the language display is not empty
    Assert.assertNotNull(languageCoding.display)
    Assert.assertFalse(languageCoding.display.isEmpty())
  }

  @Test
  fun testDeviceLanguageIdConstant() {
    // Verify the constant ID is set correctly
    Assert.assertEquals("device-language", LanguageBasicUtil.LANGUAGE_BASIC_ID)
  }

  @Test
  fun testIsLanguageWithExactMatch() {
    // Test with the current device language
    val currentLanguage = java.util.Locale.getDefault().language
    Assert.assertTrue(LanguageBasicUtil.isLanguage(currentLanguage))
  }

  @Test
  fun testIsLanguageWithNonMatchingLanguage() {
    // Test with a language that's not the current device language
    // Using a different language code that's unlikely to be the device language
    val nonMatchingLanguage =
      if (java.util.Locale.getDefault().language != "ja") "ja" else "es"
    Assert.assertFalse(LanguageBasicUtil.isLanguage(nonMatchingLanguage))
  }

}
