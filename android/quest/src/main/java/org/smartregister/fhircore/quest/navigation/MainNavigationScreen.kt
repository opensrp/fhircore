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
  val showInBottomNav: Boolean = false,
) {
  data object Home :
    MainNavigationScreen(
      org.smartregister.fhircore.engine.R.string.clients,
      org.smartregister.fhircore.engine.R.drawable.ic_home,
      "homeRoute",
      true,
    )

  data object Counters :
    MainNavigationScreen(
      R.string.counters,
      org.smartregister.fhircore.engine.R.drawable.ic_reports,
      "countersRoute",
    )

  data object Tasks :
    MainNavigationScreen(
      org.smartregister.fhircore.engine.R.string.tasks,
      org.smartregister.fhircore.engine.R.drawable.ic_tasks,
      "tasksRoute",
      true,
    )

  data object Reports :
    MainNavigationScreen(
      org.smartregister.fhircore.engine.R.string.reports,
      org.smartregister.fhircore.engine.R.drawable.ic_reports,
      "reportsRoute",
      true,
    )

  data object Settings :
    MainNavigationScreen(
      org.smartregister.fhircore.engine.R.string.settings,
      R.drawable.ic_settings,
      "settingsRoute",
      true,
    )

  data object PatientProfile :
    MainNavigationScreen(
      titleResource = org.smartregister.fhircore.engine.R.string.profile,
      route = "patientProfileRoute",
    )

  data object TracingProfile :
    MainNavigationScreen(
      titleResource = org.smartregister.fhircore.engine.R.string.profile,
      route = "tracingProfileRoute",
    )

  data object TransferOut : MainNavigationScreen(route = "transferOut")

  data object PatientGuardians : MainNavigationScreen(route = "patientProfileGuardians")

  data object FamilyProfile : MainNavigationScreen(route = "familyProfileRoute")

  data object ViewChildContacts : MainNavigationScreen(route = "viewChildContacts")

  data object GuardianProfile : MainNavigationScreen(route = "guardianProfile")

  data object TracingHistory : MainNavigationScreen(route = "tracingHistory")

  data object TracingOutcomes : MainNavigationScreen(route = "tracingOutcome")

  data object TracingHistoryDetails : MainNavigationScreen(route = "tracingHistoryDetails ")

  companion object {
    val appScreens =
      listOf(
        Home,
        Counters,
        Tasks,
        Reports,
        Settings,
        PatientProfile,
        PatientGuardians,
        FamilyProfile,
        ViewChildContacts,
        GuardianProfile,
        TransferOut,
        TracingProfile,
        TracingHistory,
        TracingOutcomes,
        TracingHistoryDetails,
      )
  }
}
