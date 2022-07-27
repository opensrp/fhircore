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
  private val overflowMenuMap: Map<OverflowMenuHost, MutableList<OverflowMenuItem>> by lazy {
    OverflowMenuHost.values().associate { Pair(it, it.overflowMenuItems.toMutableList()) }
  }

  /** Retrieve [overflowMenuHost]. Remove any menu ids that satisfy the [conditions] */
  fun retrieveOverflowMenuItems(
    overflowMenuHost: OverflowMenuHost,
    conditions: List<Pair<Int, Boolean>> = emptyList()
  ): MutableList<OverflowMenuItem> {
    val overflowMenuItems = overflowMenuMap.getValue(overflowMenuHost)
    conditions.forEach { conditionPair: Pair<Int, Boolean> ->
      overflowMenuItems.removeIf { it.id == conditionPair.first && conditionPair.second }
    }
    return overflowMenuItems
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
      OverflowMenuItem(R.id.view_family, R.string.view_family),
      OverflowMenuItem(R.id.record_as_anc, R.string.record_as_anc),
      OverflowMenuItem(
        id = R.id.remove_family_member,
        titleResource = R.string.remove_this_person,
        titleColor = DangerColor,
        confirmAction = true
      )
    )
  ),
  NEWLY_DIAGNOSED_PROFILE(
    listOf(
      OverflowMenuItem(R.id.client_guardian_visit, R.string.guardian_visit),
      OverflowMenuItem(R.id.viral_load_results, R.string.viral_load_results),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.guardian_management, R.string.guardian_management),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  ),
  ART_CLIENT_PROFILE(
    listOf(
      OverflowMenuItem(R.id.client_guardian_visit, R.string.guardian_visit),
      OverflowMenuItem(R.id.viral_load_results, R.string.viral_load_results),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.guardian_management, R.string.guardian_management),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  ),
  EXPOSED_INFANT_PROFILE(
    listOf(
      OverflowMenuItem(R.id.exposed_infant_clinic_visit, R.string.exposed_infant_clinic_visit),
      OverflowMenuItem(R.id.hiv_test_and_results, R.string.hiv_test_and_results),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.guardian_management, R.string.guardian_management),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  ),
  CHILD_CONTACT_PROFILE(
    listOf(
      OverflowMenuItem(R.id.hiv_test_and_next_appointment, R.string.hiv_test_and_next_appointment),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.guardian_management, R.string.guardian_management),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  ),
  SEXUAL_CONTACT_PROFILE(
    listOf(
      OverflowMenuItem(R.id.hiv_test_and_next_appointment, R.string.hiv_test_and_next_appointment),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  ),
  COMMUNITY_POSITIVE_PROFILE(
    listOf(
      OverflowMenuItem(R.id.hiv_test_and_next_appointment, R.string.hiv_test_and_next_appointment),
      OverflowMenuItem(R.id.edit_profile, R.string.edit_profile),
      OverflowMenuItem(R.id.guardian_management, R.string.guardian_management),
      OverflowMenuItem(R.id.clinic_history, R.string.clinic_history)
    )
  )
}
