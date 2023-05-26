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

package org.smartregister.fhircore.quest.ui

import androidx.navigation.NavHostController

sealed class StandardRegisterEvent {
  data class SearchRegister(val searchText: String = "") : StandardRegisterEvent()

  object MoveToNextPage : StandardRegisterEvent()

  object MoveToPreviousPage : StandardRegisterEvent()

  data class ApplyFilter<T>(val filterState: T) : StandardRegisterEvent()

  data class OpenProfile(val patientId: String, val navController: NavHostController) :
    StandardRegisterEvent()
}
