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
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.quest.R

class OverflowMenuFactory @Inject constructor() {
  private val overflowMenuMap: Map<OverflowMenuHost, MutableList<OverflowMenuItemConfig>> by lazy {
    OverflowMenuHost.values().associate { Pair(it, it.overflowMenuItemConfigs.toMutableList()) }
  }

  /** Retrieve [overflowMenuHost]. Remove any menu ids that satisfy the [conditions] */
  fun retrieveOverflowMenuItems(
    overflowMenuHost: OverflowMenuHost,
    conditions: List<Pair<Int, Boolean>> = emptyList()
  ): MutableList<OverflowMenuItemConfig> {
    val overflowMenuItems = overflowMenuMap.getValue(overflowMenuHost)
    conditions.forEach { conditionPair: Pair<Int, Boolean> ->
      overflowMenuItems.removeIf { it.id == conditionPair.first && conditionPair.second }
    }
    return overflowMenuItems
  }
}

/** Refers to an a screen that contains */
enum class OverflowMenuHost(val overflowMenuItemConfigs: List<OverflowMenuItemConfig>) {
  FAMILY_PROFILE(
    listOf(
      OverflowMenuItemConfig(R.id.family_details, R.string.family_details),
      OverflowMenuItemConfig(R.id.change_family_head, R.string.change_family_head),
      OverflowMenuItemConfig(R.id.family_activity, R.string.family_activity),
      OverflowMenuItemConfig(R.id.view_past_encounters, R.string.view_past_encounters),
      OverflowMenuItemConfig(
        id = R.id.remove_family,
        titleResource = R.string.remove_family,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  ),
  PATIENT_PROFILE(
    listOf(
      OverflowMenuItemConfig(R.id.individual_details, R.string.individual_details),
      OverflowMenuItemConfig(R.id.view_family, R.string.view_family),
      OverflowMenuItemConfig(R.id.record_sick_child, R.string.record_sick_child),
      OverflowMenuItemConfig(R.id.record_as_anc, R.string.record_as_anc),
      OverflowMenuItemConfig(R.id.pregnancy_outcome, R.string.pregnancy_outcome),
      OverflowMenuItemConfig(
        id = R.id.remove_family_member,
        titleResource = R.string.remove_this_person,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  )
}
