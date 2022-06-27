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

sealed class MainNavigationScreen(
  val titleResource: Int? = null,
  val iconResource: Int? = null,
  val route: String,
  val showInBottomNav: Boolean = false
) {
  object Home : MainNavigationScreen(R.string.clients, R.drawable.ic_home, "homeRoute", true)
  object Tasks : MainNavigationScreen(R.string.visits, R.drawable.ic_tasks, "tasksRoute", true)
  object Reports :
    MainNavigationScreen(R.string.reports, R.drawable.ic_reports, "reportsRoute", true)
  object Settings :
    MainNavigationScreen(R.string.settings, R.drawable.ic_settings, "settingsRoute", true)
  object PatientProfile :
    MainNavigationScreen(titleResource = R.string.profile, route = "patientProfileRoute")
  object FamilyProfile : MainNavigationScreen(route = "familyProfileRoute")

  companion object {
    val appScreens = listOf(Home, Tasks, Reports, Settings, PatientProfile, FamilyProfile)
  }
}
