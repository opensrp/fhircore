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

package org.smartregister.fhircore.engine.util.extension

import org.junit.Assert
import org.junit.Test

class StringExtensionTest {

  @Test
  fun practitionerEndpointUrlShouldMatch() {
    Assert.assertEquals(
      "PractitionerDetail?keycloak-uuid=my-keycloak-id",
      "my-keycloak-id".practitionerEndpointUrl(),
    )
  }

  @Test
  fun shouldRemoveExtraSpaces() {
    val beforeFormatExampleOne = "Aurang    zaib        umer   ,   M,           43y"
    val beforeFormatExampleTwo = "  Aurang    zaib   umer   , M, 43y          "
    val expected = "Aurang zaib umer, M, 43y"

    Assert.assertEquals(expected, beforeFormatExampleOne.removeExtraWhiteSpaces())
    Assert.assertEquals(expected, beforeFormatExampleTwo.removeExtraWhiteSpaces())
  }

  @Test
  fun stringInterpolateShouldReplaceStrings() {
    val templateString = "{ \"saveFamilyButtonText\" : @{ family.button.save } }"
    val lookupMap = mapOf<String, Any>("family.button.save" to "Save Family")

    Assert.assertEquals(
      "{ \"saveFamilyButtonText\" : Save Family }",
      templateString.interpolate(lookupMap),
    )
  }

  @Test
  fun stringInterpolateShouldCatchIllegalStateExceptionAndReturnSelf() {
    val templateString =
      "{ \"saveFamilyButtonText\" : @{ family.button.save },\"deleteFamilyButtonText\" : @{ family.button.delete } }"
    val lookupMap =
      mapOf<String, Any>(
        "family.button.save" to "@{family.button.save}",
        "family.button.delete" to "delete",
      )

    Assert.assertEquals(templateString, templateString.interpolate(lookupMap))
  }

  @Test
  fun spaceByUppercaseShouldFormatString() {
    val beforeFormatExampleOne = "QuestionnaireResponse"
    val expected = "Questionnaire Response"

    Assert.assertEquals(expected, beforeFormatExampleOne.spaceByUppercase())
  }

  @Test
  fun testRemoveHashPrefixShouldRemoveSingleHashPrefixFromString() {
    val input = "#test123"
    val result = input.removeHashPrefix()
    Assert.assertEquals("test123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldRemoveMultipleHashPrefixesFromString() {
    val input = "###test123"
    val result = input.removeHashPrefix()
    Assert.assertEquals("test123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldReturnSameStringWhenNoHashPrefixIsPresent() {
    val input = "test123"
    val result = input.removeHashPrefix()
    Assert.assertEquals("test123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleEmptyString() {
    val input = ""
    val result = input.removeHashPrefix()
    Assert.assertEquals("", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleStringWithHashSymbolsInMiddle() {
    val input = "test#123"
    val result = input.removeHashPrefix()
    Assert.assertEquals("test#123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleIntegerWithMultipleHashPrefixes() {
    val input = "###123"
    val result = input.removeHashPrefix()
    Assert.assertEquals("123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleIntegerWithoutHashPrefix() {
    val input = 123
    val result = input.toString().removeHashPrefix()
    Assert.assertEquals("123", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleOtherTypesByConvertingToString() {
    val input = 123.45
    val result = input.toString().removeHashPrefix()
    Assert.assertEquals("123.45", result)
  }

  @Test
  fun testRemoveHashPrefixShouldHandleUuidWithMultipleHashPrefixes() {
    val input = "##8036ea0d-da4f-435c-bd4a-3e819a5a52dc"
    val result = input.removeHashPrefix()
    Assert.assertEquals("8036ea0d-da4f-435c-bd4a-3e819a5a52dc", result)
  }
}
