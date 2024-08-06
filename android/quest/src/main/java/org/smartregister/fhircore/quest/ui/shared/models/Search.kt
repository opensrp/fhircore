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

package org.smartregister.fhircore.quest.ui.shared.models

import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger

sealed class SearchMode {
  data object KeyboardInput : SearchMode()

  data object QrCodeScan : SearchMode()
}

/**
 * Wrapper class to hold search input [String] and the [SearchMode] mode used to initiate the UI
 * search
 *
 * Depending on the [SearchMode], additional [ActionTrigger.ON_SEARCH_SINGLE_RESULT] actions can be
 * triggered a query returns a single result
 *
 * @param query Actual search input string
 * @param mode [SearchMode] that initiated the search
 */
data class SearchQuery(val query: String, val mode: SearchMode = SearchMode.KeyboardInput) {

  fun isBlank() = query.isBlank()

  companion object {
    val emptyText = SearchQuery(query = "")
  }
}
