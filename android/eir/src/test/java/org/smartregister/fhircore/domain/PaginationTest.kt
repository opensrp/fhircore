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

package org.smartregister.fhircore.domain

import org.junit.Assert
import org.junit.Test

class PaginationTest {

  private val pagination = Pagination(100, 10, 1)

  @Test
  fun testHasNextPageShouldReturnFalse() {
    Assert.assertFalse(pagination.hasNextPage())
  }

  @Test
  fun testCurrentPageNumberShouldReturnValidCount() {
    Assert.assertEquals(2, pagination.currentPageNumber())
  }

  @Test
  fun testTotalPagesShouldReturnValidCount() {
    Assert.assertEquals(10, pagination.totalPages())
  }

  @Test
  fun testHasPreviousPageShouldReturnFalse() {
    Assert.assertFalse(pagination.hasPreviousPage())
  }
}
