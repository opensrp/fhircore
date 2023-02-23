/*
 * Copyright 2021-2023 Ona Systems, Inc
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
  val route: Int,
  val showInBottomNav: Boolean = false
) {
  object Home :
    MainNavigationScreen(R.string.clients, R.drawable.ic_home, R.id.registerFragment, true)
  object Reports :
    MainNavigationScreen(R.string.reports, R.drawable.ic_reports, R.id.measureReportFragment, true)
  object Settings :
    MainNavigationScreen(R.string.settings, R.drawable.ic_settings, R.id.userSettingFragment, true)
  object Profile :
    MainNavigationScreen(titleResource = R.string.profile, route = R.id.profileFragment)
  object GeoWidget : MainNavigationScreen(route = R.id.geoWidgetFragment)
}
