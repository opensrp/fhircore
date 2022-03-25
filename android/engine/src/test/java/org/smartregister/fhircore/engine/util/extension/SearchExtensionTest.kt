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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class SearchExtensionTest {

  @Test
  fun testSearchFilterByPatientShouldAddReferenceFilter() {
    val search = Search(ResourceType.Patient)

    search.filterByResourceTypeId(ReferenceClientParam("link"), ResourceType.Patient, "123344")

    val referenceFilterParamCriterion: MutableList<Any> =
      ReflectionHelpers.getField(search, "referenceFilterCriteria")
    val referenceFilters: MutableList<ReferenceParamFilterCriterion> =
      ReflectionHelpers.getField(referenceFilterParamCriterion[0], "filters")

    assertEquals(1, referenceFilters.size)
    assertEquals("Patient/123344", referenceFilters[0].value)
    assertEquals("link", referenceFilters[0].parameter?.paramName)
  }
}
