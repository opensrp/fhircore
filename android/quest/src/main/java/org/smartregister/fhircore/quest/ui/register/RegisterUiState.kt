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

package org.smartregister.fhircore.quest.ui.register

import com.google.android.fhir.sync.CurrentSyncJobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.domain.model.ActionParameter

data class RegisterUiState(
  val screenTitle: String = "",
  val isFirstTimeSync: Boolean = false,
  val registerConfiguration: RegisterConfiguration? = null,
  val registerId: String = "",
  val totalRecordsCount: Long = 0,
  val filteredRecordsCount: Long = 0,
  val pagesCount: Int = 1,
  val progressPercentage: Flow<Int> = flowOf(0),
  val isSyncUpload: Flow<Boolean> = flowOf(false),
  val currentSyncJobStatus: Flow<CurrentSyncJobStatus?> = flowOf(null),
  val params: List<ActionParameter> = emptyList(),
)
