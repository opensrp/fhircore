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

package org.smartregister.fhircore.engine.data.domain.util

import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class PaginatedDataSourceTest : RobolectricTest() {

  private lateinit var paginatedDataSource: PaginatedDataSource<String, String>
  private lateinit var registerRepository: RegisterRepository<String, String>

  @Before
  fun setUp() {
    registerRepository = mockk()
    paginatedDataSource = PaginatedDataSource(registerRepository)
    paginatedDataSource.loadAll = true
  }

  @Test
  fun testLoadDataFromRepository() = runBlocking {
    coEvery { registerRepository.loadData(any(), any(), any()) } returns listOf("data")
    val result =
      paginatedDataSource.load(PagingSource.LoadParams.Refresh(1, 1, true)) as
        PagingSource.LoadResult.Page<String, String>

    assertEquals(1, result.data.size)
    assertEquals("data", result.data[0])
    assertEquals(0, result.prevKey)
    assertNull(result.nextKey)
  }

  @Test
  fun testGetRefreshKeyShouldReturnAnchorPosition() {
    assertEquals(
      10,
      paginatedDataSource.getRefreshKey(mockk { every { anchorPosition } returns 10 })
    )
  }
}
