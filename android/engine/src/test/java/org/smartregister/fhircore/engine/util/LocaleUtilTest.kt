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

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class LocaleUtilTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configRegistry: ConfigurationRegistry

  private lateinit var localeUtil: LocaleUtil

  @Before
  fun setUp() {

    hiltRule.inject()
    localeUtil = LocaleUtil(configRegistry)
  }

  @Test
  fun testParseTemplateWithDefaultLocaleGeneratesCorrectlyTranslatedString() {

    val templateString = "{{person.gender}} from {{person.address}}"

    val result =
      localeUtil.parseTemplate(LocaleUtil.STRINGS_BASE_BUNDLE_NAME, Locale.ENGLISH, templateString)

    Assert.assertEquals("Male from Nairobi, Kenya", result)
  }

  @Test
  fun testParseTemplateWithOtherLocaleGeneratesCorrectlyTranslatedString() {

    val templateString = "{{person.gender}} from {{person.address}}"

    val result =
      localeUtil.parseTemplate(LocaleUtil.STRINGS_BASE_BUNDLE_NAME, Locale.FRENCH, templateString)

    Assert.assertEquals("Mâle from Paris, France", result)
  }

  @Test
  fun testParseTemplateWithArgumentsGeneratesCorrectlyTranslatedString() {

    val templateString = "{{person.age.years}}"

    val result =
      localeUtil.parseTemplate(LocaleUtil.STRINGS_BASE_BUNDLE_NAME, Locale.ENGLISH, templateString)

    Assert.assertEquals("Age is 4 years", localeUtil.formatMessage(Locale.ENGLISH, result, "4"))
  }

  @Test
  fun testGenerateIdentifierReturnsCorrectKey() {

    val result = localeUtil.generateIdentifier("OVERDUE")
    Assert.assertEquals("overdue", result)
  }

  @Test
  fun testGenerateIdentifierWithDigitPrefixParamReturnsCorrectKey() {
    val result = localeUtil.generateIdentifier("40 Weeks")
    Assert.assertEquals("_40_weeks", result)
  }

  @Test
  fun testGenerateIdentifierWithWhitespacesParamReturnsCorrectKey() {

    val result = localeUtil.generateIdentifier("Home Address")
    Assert.assertEquals("home_address", result)
  }
}
