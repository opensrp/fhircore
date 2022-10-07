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

import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.smartregister.fhircore.engine.appfeature.model.HealthModule

object NavigationArg {

  const val FAMILY_ID = "familyId"
  const val FEATURE = "feature"
  const val HEALTH_MODULE = "healthModule"
  const val SCREEN_TITLE = "screenTitle"
  const val PATIENT_ID = "patientId"
  const val ON_ART = "onART"

  fun commonNavArgs(appFeatureName: String, healthModule: HealthModule) =
    mutableListOf(
      navArgument(FEATURE) {
        type = NavType.StringType
        nullable = true
        defaultValue = appFeatureName
      },
      navArgument(HEALTH_MODULE) {
        type = NavType.EnumType(HealthModule::class.java)
        nullable = false
        defaultValue = healthModule
      }
    )

  /** Create route paths */
  fun routePathsOf(includeCommonArgs: Boolean = false, vararg navArg: String): String =
    "?" +
      if (includeCommonArgs) listOf(FEATURE, HEALTH_MODULE).plus(navArg).joinByAmpersand()
      else navArg.toList().joinByAmpersand()

  private fun List<String>.joinByAmpersand() = this.joinToString("&") { "$it={$it}" }

  /** Bind nav arguments values */
  fun bindArgumentsOf(vararg navArg: Pair<String, String?>): String =
    "?" + navArg.joinToString("&") { "${it.first}=${it.second}" }
}
