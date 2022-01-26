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

package org.smartregister.fhircore.anc.util

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.StringParamFilterCriterion
import com.google.android.fhir.search.filter.TokenFilterValue
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.robolectric.util.ReflectionHelpers

class SearchExtensionTest {

  @Test
  fun testSearchFilterByShouldAddTokenFilterOfCodingType() {
    val filter =
      SearchFilter(
        "_tag",
        Enumerations.SearchParamType.TOKEN,
        Enumerations.DataType.CODING,
        valueCoding = Coding("http://snomed.com", "123456", "Code")
      )

    val search = Search(ResourceType.Patient)

    search.filterBy(filter)

    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")

    val tokenValue =
      ReflectionHelpers.getField<List<TokenFilterValue>>(tokenFilters[0].value, "tokenFilters")
    val tokenMap =
      getParametersMap(
        tokenValue.toString().substringAfter("(").substringBefore(")").filter { !it.isWhitespace() }
      )

    assertEquals(1, tokenFilters.size)
    assertEquals("http://snomed.com", tokenMap["uri"])
    assertEquals("123456", tokenMap["code"])
    assertEquals("_tag", tokenFilters[0].parameter?.paramName)
  }

  @Test
  fun testSearchFilterByShouldAddTokenFilterOfCodeableConceptType() {
    val filter =
      SearchFilter(
        "code",
        Enumerations.SearchParamType.TOKEN,
        Enumerations.DataType.CODEABLECONCEPT,
        valueCoding = Coding("http://snomed.com", "123456", "Code")
      )

    val search = Search(ResourceType.Patient)

    search.filterBy(filter)

    val tokenFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "tokenFilterCriteria")
    val tokenFilters: MutableList<TokenParamFilterCriterion> =
      ReflectionHelpers.getField(tokenFilterParamCriterion[0], "filters")

    val tokenValue =
      ReflectionHelpers.getField<List<TokenFilterValue>>(tokenFilters[0].value, "tokenFilters")
    val tokenMap =
      getParametersMap(
        tokenValue.toString().substringAfter("(").substringBefore(")").filter { !it.isWhitespace() }
      )

    assertEquals(1, tokenFilters.size)
    assertEquals("http://snomed.com", tokenMap["uri"])
    assertEquals("123456", tokenMap["code"])
    assertEquals("code", tokenFilters[0].parameter?.paramName)
  }

  @Test
  fun testSearchFilterByShouldAddTokenFilterOfStringType() {
    val filter =
      SearchFilter(
        "address-city",
        Enumerations.SearchParamType.STRING,
        Enumerations.DataType.STRING,
        valueString = "NAIROBI"
      )

    val search = Search(ResourceType.Patient)

    search.filterBy(filter)

    val stringFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "stringFilterCriteria")
    val stringFilters: MutableList<StringParamFilterCriterion> =
      ReflectionHelpers.getField(stringFilterParamCriterion[0], "filters")

    assertEquals(1, stringFilters.size)
    assertEquals("NAIROBI", stringFilters[0].value)
    assertEquals(StringFilterModifier.MATCHES_EXACTLY, stringFilters[0].modifier)

    assertEquals("address-city", stringFilters[0].parameter?.paramName)
  }

  @Test
  fun testSearchFilterByShouldThrowExceptionForUnhandledFilterType() {
    val filter =
      SearchFilter("birthdate", Enumerations.SearchParamType.DATE, Enumerations.DataType.STRING)

    val search = Search(ResourceType.Patient)

    val ex = assertThrows<UnsupportedOperationException> { search.filterBy(filter) }

    assertEquals("Can not apply DATE as search filter", ex.message)
  }

  @Test
  fun testSearchFilterByShouldThrowExceptionForUnhandledDataType() {
    val filter =
      SearchFilter("birthdate", Enumerations.SearchParamType.TOKEN, Enumerations.DataType.STRING)

    val search = Search(ResourceType.Patient)

    val ex = assertThrows<UnsupportedOperationException> { search.filterBy(filter) }

    assertEquals("SDK does not support value type STRING", ex.message)
  }

  @Test
  fun testSearchFilterByPatientShouldAddReferenceFilter() {
    val search = Search(ResourceType.Patient)

    search.filterByPatient(ReferenceClientParam("link"), "123344")

    val referenceFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "referenceFilterCriteria")
    val referenceFilters: MutableList<ReferenceParamFilterCriterion> =
      ReflectionHelpers.getField(referenceFilterParamCriterion[0], "filters")

    assertEquals(1, referenceFilters.size)
    assertEquals("Patient/123344", referenceFilters[0].value)
    assertEquals("link", referenceFilters[0].parameter?.paramName)
  }

  @Test
  fun testFilterByPatientNameShouldAddStringFilter() {
    val search = Search(ResourceType.Patient)
    search.filterByPatientName("John")

    val stringFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "stringFilterCriteria")
    val stringFilters: MutableList<StringParamFilterCriterion> =
      ReflectionHelpers.getField(stringFilterParamCriterion[0], "filters")

    assertEquals(1, stringFilters.size)
    assertEquals("John", stringFilters[0].value)
    assertEquals(StringFilterModifier.CONTAINS, stringFilters[0].modifier)
  }

  private fun getParametersMap(mapAsString: String) =
    mapAsString.substringAfter("(").substringBefore(")").split(",").associateTo(HashMap()) {
      val (left, right) = it.split("=")
      left to right
    }
}
