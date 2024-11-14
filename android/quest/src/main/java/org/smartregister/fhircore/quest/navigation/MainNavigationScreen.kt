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

package org.smartregister.fhircore.quest.navigation

import org.smartregister.fhircore.engine.R

sealed class MainNavigationScreen(
  val titleResource: Int? = null,
  val iconResource: Int? = null,
  val route: Int,
  val showInBottomNav: Boolean = false,
) {
  data object Home :
    MainNavigationScreen(
      R.string.clients,
      org.smartregister.fhircore.quest.R.drawable.ic_home,
      org.smartregister.fhircore.quest.R.id.registerFragment,
      true,
    )

  data object Reports :
    MainNavigationScreen(
      R.string.reports,
      R.drawable.ic_reports,
      org.smartregister.fhircore.quest.R.id.measureReportFragment,
      true,
    )

  data object Settings :
    MainNavigationScreen(
      R.string.settings,
      R.drawable.ic_settings,
      org.smartregister.fhircore.quest.R.id.userSettingFragment,
      true,
    )

  data object Profile :
    MainNavigationScreen(
      titleResource = R.string.profile,
      route = org.smartregister.fhircore.quest.R.id.profileFragment,
    )

  data object GeoWidgetLauncher :
    MainNavigationScreen(route = org.smartregister.fhircore.quest.R.id.geoWidgetLauncherFragment)

  data object Insight :
    MainNavigationScreen(route = org.smartregister.fhircore.quest.R.id.userInsightScreenFragment)

  data object LocationSelector :
    MainNavigationScreen(
      route = org.smartregister.fhircore.quest.R.id.multiSelectBottomSheetFragment,
    )

  data object SummaryBottomSheetFragment :
    MainNavigationScreen(
      route = org.smartregister.fhircore.quest.R.id.summaryBottomSheetFragment,
    )

  data object AlertDialogFragment :
    MainNavigationScreen(
      route = org.smartregister.fhircore.quest.R.id.alertDialogFragment,
    )

  fun eventId(id: String) = route.toString() + "_" + id
}
