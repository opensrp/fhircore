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
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
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
      """{"id":"householdQueryByType","filterType":"TOKEN","key":"type","valueType":"STRING","valueString":"Value"}""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterString(dataQuery)
    val stringClientParamSlot = slot<StringClientParam>()
    verify { search.filter(capture(stringClientParamSlot), any()) }
    Assert.assertEquals(dataQuery.key, stringClientParamSlot.captured.paramName)
  }

  @Test
  fun testFilterStringExtensionForBooleanType() {
    val dataQuery =
      """{"id":"householdQueryByType","filterType":"TOKEN","key":"type","valueType":"BOOLEAN","valueBoolean":"true"}""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterString(dataQuery)
    val stringClientParamSlot = slot<StringClientParam>()
    verify { search.filter(capture(stringClientParamSlot), any()) }
    Assert.assertEquals(dataQuery.key, stringClientParamSlot.captured.paramName)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun testUnknownTypeFilterThrowsException() {
    val dataQuery =
      """{"id":"householdQueryByType","filterType":"TOKEN","key":"type","valueType":"AGE","valueBoolean":"true"}""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterString(dataQuery)
  }

  @Test
  fun testFilterForDateType() {
    val dataQuery =
      """{
          "id": "childQueryByDate",
          "filterType": "DATE",
          "key": "birthdate",
          "valueType": "DATE",
          "valueDate": "2017-03-14",
          "paramPrefix": "GREATERTHAN_OR_EQUALS"
        }""".decodeJson<
        DataQuery>()
    val search = spyk(Search(ResourceType.Patient))
    search.filterDate(dataQuery)
    val dateClientParamSlot = slot<DateClientParam>()
    verify { search.filter(capture(dateClientParamSlot), any()) }
    Assert.assertEquals(dataQuery.key, dateClientParamSlot.captured.paramName)
  }
}
