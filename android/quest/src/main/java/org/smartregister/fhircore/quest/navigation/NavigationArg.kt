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

object NavigationArg {

  const val REGISTER_ID = "registerId"
  const val PROFILE_ID = "profileId"
  const val FEATURE = "feature"
  const val SCREEN_TITLE = "screenTitle"
  const val RESOURCE_ID = "resourceId"
  const val RESOURCE_CONFIG = "resourceConfig"
  const val CONFIG_ID = "configId"
  const val REPORT_ID = "reportId"
  const val actionParams = "params"
  const val TOOL_BAR_HOME_NAVIGATION = "toolBarHomeNavigation"

  /** Create route paths */
  fun routePathsOf(vararg navArg: String): String =
    "?" + navArg.toList().joinToString("&") { "$it={$it}" }

  /** Bind nav arguments values */
  fun bindArgumentsOf(vararg navArg: Pair<String, String?>): String =
    "?" + navArg.joinToString("&") { "${it.first}=${it.second}" }
}
