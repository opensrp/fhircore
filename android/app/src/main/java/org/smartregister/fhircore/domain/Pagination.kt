/*
 * Copyright 2021 Ona Systems Inc
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

data class Pagination(val totalItems: Int, val pageSize: Int, var currentPage: Int)

fun Pagination.hasNextPage(): Boolean {
  return this.totalPages() > 0 && this.currentPage == (this.totalPages() - 1)
}

fun Pagination.currentPageNumber(): Int {
  return this.currentPage + 1
}

fun Pagination.totalPages(): Int {
  return this.totalItems / this.pageSize +
    (if (this.totalItems % this.pageSize > 0) 1 else 0) +
    if (this.totalItems == 0) 1 else 0
}

fun Pagination.hasPreviousPage(): Boolean {
  return this.currentPage == 0
}
