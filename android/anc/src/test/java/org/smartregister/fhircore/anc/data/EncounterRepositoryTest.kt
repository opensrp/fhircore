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

package org.smartregister.fhircore.anc.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.mockk
import java.text.SimpleDateFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Period
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
class EncounterRepositoryTest : RobolectricTest() {

  private lateinit var repository: EncounterRepository

  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()
    repository = EncounterRepository(fhirEngine).apply { patientId = "1" }
  }

  @Test
  fun testGetRefreshKeyShouldReturnValidAnchorPosition() {
    val pagingState =
      PagingState<Int, EncounterItem>(
        pages = listOf(),
        anchorPosition = 0,
        config = PagingConfig(1),
        leadingPlaceholderCount = 10
      )
    val anchorPosition = repository.getRefreshKey(pagingState)
    Assert.assertEquals(0, anchorPosition)
  }

  @Test
  fun testLoadReturnsPageWhenOnSuccessfulLoadOfItemKeyedData() = runBlockingTest {
    val encounter = getEncounter()

    coEvery {
      hint(Encounter::class)
      fhirEngine.search<Encounter>(any())
    } returns listOf(encounter)

    Assert.assertEquals(
      PagingSource.LoadResult.Page(
        listOf(
          EncounterItem(
            id = encounter.id,
            status = encounter.status,
            display = encounter.class_.display,
            periodStartDate = encounter.period.start
          )
        ),
        null,
        1
      ),
      repository.load(PagingSource.LoadParams.Refresh(null, 1, false))
    )

    encounter.class_ = null

    coEvery {
      hint(Encounter::class)
      fhirEngine.search<Encounter>(any())
    } returns listOf(encounter)

    val result = repository.load(PagingSource.LoadParams.Refresh(null, 1, false))

    Assert.assertTrue(result is PagingSource.LoadResult.Error)
    Assert.assertEquals(
      NullPointerException::class.simpleName,
      (result as PagingSource.LoadResult.Error).throwable.javaClass.simpleName
    )
  }

  private fun getEncounter(): Encounter {
    return Encounter().apply {
      id = "1"
      status = Encounter.EncounterStatus.FINISHED
      class_ = Coding("", "", "first")
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01") }
    }
  }
}
