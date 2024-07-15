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

sealed class UiSearchMode {
  data object KeyboardInput : UiSearchMode()

  data object QrCodeScan : UiSearchMode()
}

data class UiSearchQuery(val query: String, val mode: UiSearchMode = UiSearchMode.KeyboardInput) {
  fun isEmpty() = query.isEmpty()

  fun isBlank() = query.isBlank()

  companion object {
    val emptyText = UiSearchQuery(query = "")
  }
}
