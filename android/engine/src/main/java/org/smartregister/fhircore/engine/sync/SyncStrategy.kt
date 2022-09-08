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

package org.smartregister.fhircore.engine.sync

import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.ApplicationUtil

/**
 * SyncStrategy defines whether to sync resource based on the IDs of CareTeam, Location,
 * Organization and Practitioner. Each SyncStrategy represents a meta tag that is used by all synced
 * resource.
 */
enum class SyncStrategy(val value: String, val tag: Coding) {
  CARETEAM(
    value = "CareTeam",
    tag =
      Coding().apply {
        system = ApplicationUtil.application.getString(R.string.sync_strategy_careteam_system)
        display = ApplicationUtil.application.getString(R.string.sync_strategy_careteam_display)
      }
  ),
  LOCATION(
    value = "Location",
    tag =
      Coding().apply {
        system = ApplicationUtil.application.getString(R.string.sync_strategy_location_system)
        display = ApplicationUtil.application.getString(R.string.sync_strategy_location_display)
      }
  ),
  ORGANIZATION(
    value = "Organization",
    tag =
      Coding().apply {
        system = ApplicationUtil.application.getString(R.string.sync_strategy_organization_system)
        display = ApplicationUtil.application.getString(R.string.sync_strategy_organization_display)
      }
  ),
  PRACTITIONER(
    value = "Practitioner",
    tag =
      Coding().apply {
        system = ApplicationUtil.application.getString(R.string.sync_strategy_practitioner_system)
        display = ApplicationUtil.application.getString(R.string.sync_strategy_practitioner_display)
      }
  )
}
