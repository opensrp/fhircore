/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.QuantityClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.gclient.UriClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.domain.model.DataQuery

class SearchExtensionTest {

  @Test
  fun testSearchFilterByPatientShouldAddReferenceFilter() {
    val search = Search(ResourceType.Patient)

    search.filterByResourceTypeId(ReferenceClientParam("link"), ResourceType.Patient, "123344")

    val referenceFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "referenceFilterCriteria")
    val referenceFilters: MutableList<ReferenceParamFilterCriterion> =
      ReflectionHelpers.getField(referenceFilterParamCriterion[0], "filters")

    Assert.assertEquals(1, referenceFilters.size)
    Assert.assertEquals("Patient/123344", referenceFilters[0].value)
    Assert.assertEquals("link", referenceFilters[0].parameter.paramName)
  }

  @Test
  fun testFilterStringExtensionForStringType() {
    val dataQuery =
      """{
          "paramName":"type", 
          "filterCriteria": [
            {
              "dataType": "STRING",
              "value": "Value"
            }
          ]
         }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val stringClientParamSlot = slot<StringClientParam>()
    verify {
      search.filter(
        stringParameter = capture(stringClientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, stringClientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterByDataTypeNumber() {
    val dataQuery =
      """{
          "paramName":"type", 
          "filterCriteria": [
            {
              "dataType": "INTEGER",
              "value": 10
            }
          ]
         }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val clientParamSlot = slot<NumberClientParam>()
    verify {
      search.filter(
        numberParameter = capture(clientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, clientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterByDataTypeQuantity() {
    val dataQuery =
      """{
          "paramName":"type", 
          "filterCriteria": [
            {
              "dataType": "QUANTITY",
              "value": 10
            }
          ]
         }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val clientParamSlot = slot<QuantityClientParam>()
    verify {
      search.filter(
        quantityParameter = capture(clientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, clientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterByDataTypeReference() {
    val dataQuery =
      """{
          "paramName":"type", 
          "filterCriteria": [
            {
              "dataType": "REFERENCE",
              "value": "Patient/sample-logical-id"
            }
          ]
         }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val clientParamSlot = slot<ReferenceClientParam>()
    verify {
      search.filter(
        referenceParameter = capture(clientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, clientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterByDataTypeUri() {
    val dataQuery =
      """{
          "paramName":"type", 
          "filterCriteria": [
            {
              "dataType": "URI",
              "value": "http://sample-dummy-uri.com"
            }
          ]
         }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val clientParamSlot = slot<UriClientParam>()
    verify {
      search.filter(uriParam = capture(clientParamSlot), init = anyVararg(), operation = any())
    }
    Assert.assertEquals(dataQuery.paramName, clientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterStringExtensionForBooleanType() {
    val dataQuery =
      """{
          "paramName": "type",
          "filterCriteria": [
            {
              "dataType": "CODE",
              "value": {
                "code": "true"
              }
            }
          ]
        }"""
        .trimMargin()
        .decodeJson<DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val stringClientParamSlot = slot<TokenClientParam>()
    verify {
      search.filter(
        tokenParameter = capture(stringClientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, stringClientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterForDateType() {
    val dataQuery =
      """{
          "paramName": "birthdate",
          "filterCriteria": [
            {
              "dataType": "DATE",
              "valueDate": "2017-03-14",
              "prefix": "GREATERTHAN_OR_EQUALS"
            }
          ]
        }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterBy(dataQuery)
    val dateClientParamSlot = slot<DateClientParam>()
    verify {
      search.filter(
        dateParameter = capture(dateClientParamSlot),
        init = anyVararg(),
        operation = any()
      )
    }
    Assert.assertEquals(dataQuery.paramName, dateClientParamSlot.captured.paramName)
  }
}
