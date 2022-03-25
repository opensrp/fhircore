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

package org.smartregister.fhircore.engine.ui.main

import java.util.Locale
import org.smartregister.fhircore.engine.domain.model.SideMenuOption

data class AppMainUiState(
  val appTitle: String,
  val username: String,
  val lastSyncTime: String,
  val language: String,
  val sideMenuOptions: List<SideMenuOption>
)

fun appMainUiStateOf(
  appTitle: String = "FHIR App",
  username: String = "",
  lastSyncTime: String = "",
  language: String = Locale.ENGLISH.displayName,
  sideMenuOptions: List<SideMenuOption> = emptyList()
): AppMainUiState {
  return AppMainUiState(
    appTitle = appTitle,
    username = username,
    lastSyncTime = lastSyncTime,
    language = language,
    sideMenuOptions = sideMenuOptions
  )
}
