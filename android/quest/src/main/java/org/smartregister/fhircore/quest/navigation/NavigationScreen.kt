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

package org.smartregister.fhircore.quest.navigation

import org.smartregister.fhircore.quest.R

sealed class NavigationScreen(
  val titleResource: Int? = null,
  val iconResource: Int? = null,
  val route: String,
  val showInBottomNav: Boolean = false
) {
  object Home : NavigationScreen(R.string.clients, R.drawable.ic_home, HOME_ROUTE, true)
  object Tasks : NavigationScreen(R.string.tasks, R.drawable.ic_tasks, TASKS_ROUTE, true)
  object Reports : NavigationScreen(R.string.reports, R.drawable.ic_reports, REPORTS_ROUTE, true)
  object Settings :
    NavigationScreen(R.string.settings, R.drawable.ic_settings, SETTINGS_ROUTE, true)
  object PatientProfile :
    NavigationScreen(titleResource = R.string.profile, route = PATIENT_PROFILE_ROUTE)
  object FamilyProfile : NavigationScreen(route = FAMILY_PROFILE_ROUTE)

  companion object {
    const val HOME_ROUTE = "homeRoute"
    const val TASKS_ROUTE = "tasksRoute"
    const val REPORTS_ROUTE = "reportsRoute"
    const val SETTINGS_ROUTE = "settingsRoute"
    const val PATIENT_PROFILE_ROUTE = "patientProfileRoute"
    const val FAMILY_PROFILE_ROUTE = "familyProfileRoute"

    val appScreens = listOf(Home, Tasks, Reports, Settings, PatientProfile, FamilyProfile)
  }
}
