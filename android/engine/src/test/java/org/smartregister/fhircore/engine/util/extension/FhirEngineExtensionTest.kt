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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.SearchQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.sql.SQLException
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class FhirEngineExtensionTest : RobolectricTest() {

  private val fhirEngine: FhirEngine = mockk()

  @Test
  fun searchCompositionByIdentifier() = runBlocking {
    coEvery { fhirEngine.search<Composition>(any<Search>()) } returns
      listOf(Composition().apply { id = "123" })

    val result = fhirEngine.searchCompositionByIdentifier("appId")

    coVerify { fhirEngine.search<Composition>(any<Search>()) }

    Assert.assertEquals("123", result!!.logicalId)
  }

  @Test
  fun testAddDateTimeIndexThrowsExceptionGivenInvalidSearchQuery() {
    coEvery { fhirEngine.search<Task>(any<SearchQuery>()) } throws SQLException()

    runBlocking { assertThrows<SQLException> { fhirEngine.addDateTimeIndex() } }
  }

  @Test
  fun testAddDateTimeIndexUsesCorrectSql() {
    coEvery { fhirEngine.search<Task>(any<SearchQuery>()) } returns listOf()

    runBlocking { fhirEngine.addDateTimeIndex() }
    val searchQuerySlot = slot<SearchQuery>()
    coVerify { fhirEngine.search<Task>(capture(searchQuerySlot)) }

    Assert.assertEquals(
      "CREATE INDEX `index_DateTimeIndexEntity_index_from` ON `DateTimeIndexEntity` (`index_from`)",
      searchQuerySlot.captured.query
    )
  }
}
