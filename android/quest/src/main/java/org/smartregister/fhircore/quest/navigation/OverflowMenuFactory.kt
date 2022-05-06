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

import javax.inject.Inject
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItem
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.quest.R

class OverflowMenuFactory @Inject constructor() {
  val overflowMenuMap: Map<OverflowMenuHost, List<OverflowMenuItem>> by lazy {
    OverflowMenuHost.values().associate { Pair(it, it.overflowMenuItems) }
  }
}

/** Refers to an a screen that contains */
enum class OverflowMenuHost(val overflowMenuItems: List<OverflowMenuItem>) {
  FAMILY_PROFILE(
    listOf(
      OverflowMenuItem(R.id.family_details, R.string.family_details),
      OverflowMenuItem(R.id.change_family_head, R.string.change_family_head),
      OverflowMenuItem(R.id.family_activity, R.string.family_activity),
      OverflowMenuItem(R.id.view_past_encounters, R.string.view_past_encounters),
      OverflowMenuItem(
        id = R.id.remove_family,
        titleResource = R.string.remove_family,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  ),
  PATIENT_PROFILE(
    listOf(
      OverflowMenuItem(R.id.individual_details, R.string.individual_details),
      OverflowMenuItem(
        id = R.id.remove_family_member,
        titleResource = R.string.remove_this_person,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  )
}
