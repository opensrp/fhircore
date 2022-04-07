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

import javax.inject.Inject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItem
import org.smartregister.fhircore.engine.ui.theme.DangerColor

class OverflowMenuFactory @Inject constructor() {
  val overflowMenuMap: MutableMap<OverflowMenuHost, List<OverflowMenuItem>> by lazy {
    mutableMapOf(
      Pair(OverflowMenuHost.FAMILY_PROFILE, OverflowMenuHost.FAMILY_PROFILE.overflowMenuItems)
    )
  }
}

/** Refers to an a screen that contains */
enum class OverflowMenuHost(val overflowMenuItems: List<OverflowMenuItem>) {
  FAMILY_PROFILE(
    listOf(
      OverflowMenuItem(FamilyProfileMenuConstant.FAMILY_DETAILS, R.string.family_details),
      OverflowMenuItem(FamilyProfileMenuConstant.CHANGE_FAMILY_HEAD, R.string.change_family_head),
      OverflowMenuItem(
        FamilyProfileMenuConstant.CHANGE_PRIMARY_CAREGIVER,
        R.string.change_primary_caregiver
      ),
      OverflowMenuItem(FamilyProfileMenuConstant.FAMILY_ACTIVITY, R.string.family_activity),
      OverflowMenuItem(
        FamilyProfileMenuConstant.VIEW_PAST_ENCOUNTERS,
        R.string.view_past_encounters
      ),
      OverflowMenuItem(
        id = FamilyProfileMenuConstant.REMOVE_FAMILY,
        titleResource = R.string.remove_family,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  )
}

object FamilyProfileMenuConstant {
  const val FAMILY_DETAILS = "familyDetails"
  const val CHANGE_FAMILY_HEAD = "changeFamilyHead"
  const val CHANGE_PRIMARY_CAREGIVER = "changePrimaryCaregiver"
  const val FAMILY_ACTIVITY = "familyActivity"
  const val VIEW_PAST_ENCOUNTERS = "viewPastEncounters"
  const val REMOVE_FAMILY = "removeFamily"
}
