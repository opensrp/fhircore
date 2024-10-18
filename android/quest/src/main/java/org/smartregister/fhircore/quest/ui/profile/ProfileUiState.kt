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

package org.smartregister.fhircore.quest.ui.profile

import org.smartregister.fhircore.engine.configuration.app.SnackBarThemeConfig
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData

data class ProfileUiState(
  val resourceData: ResourceData? = null,
  val profileConfiguration: ProfileConfiguration? = null,
  val snackBarTheme: SnackBarThemeConfig = SnackBarThemeConfig(),
  val showDataLoadProgressIndicator: Boolean = true,
)
