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

package org.smartregister.fhircore.engine.navigation

import org.smartregister.fhircore.engine.R

sealed class NavigationScreen(
  val titleResource: Int,
  val iconResource: Int? = null,
  val route: String
) {
  object Home : NavigationScreen(R.string.clients, R.drawable.ic_home, HOME_ROUTE)
  object Tasks : NavigationScreen(R.string.tasks, R.drawable.ic_tasks, TASKS_ROUTE)
  object Reports : NavigationScreen(R.string.reports, R.drawable.ic_reports, REPORTS_ROUTE)
  object Settings : NavigationScreen(R.string.settings, R.drawable.ic_settings, SETTINGS_ROUTE)

  companion object {
    const val HOME_ROUTE = "homeRoute"
    const val TASKS_ROUTE = "tasksRoute"
    const val REPORTS_ROUTE = "reportsRoute"
    const val SETTINGS_ROUTE = "settingsRoute"
  }
}
