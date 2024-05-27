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

object NavigationArg {

  const val REGISTER_ID = "registerId"
  const val PROFILE_ID = "profileId"
  const val SCREEN_TITLE = "screenTitle"
  const val RESOURCE_ID = "resourceId"
  const val RESOURCE_CONFIG = "resourceConfig"
  const val MULTI_SELECT_VIEW_CONFIG = "multiSelectViewConfig"
  const val CONFIG_ID = "configId"
  const val GEO_WIDGET_ID = "geoWidgetId"
  const val DETAILS_BOTTOM_SHEET_CONFIG = "detailsBottomSheetConfig"
  const val REPORT_ID = "reportId"
  const val PARAMS = "params"
  const val TOOL_BAR_HOME_NAVIGATION = "toolBarHomeNavigation"
  const val LAUNCHER_TYPE = "launcherType"

  /** Create route paths */
  fun routePathsOf(vararg navArg: String): String =
    "?" + navArg.toList().joinToString("&") { "$it={$it}" }

  /** Bind nav arguments values */
  fun bindArgumentsOf(vararg navArg: Pair<String, String?>): String =
    "?" + navArg.joinToString("&") { "${it.first}=${it.second}" }
}
