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

package org.smartregister.fhircore.quest.ui.shared.models

/**
 * This sealed class is used to represent various click events of the configurable view components
 */
sealed class ViewComponentEvent {

  /**
   * Event triggered when user clicks a service card. Uses [profileId] to fetch the profile
   * configurations and [resourceId] to fetch the data for the current profile.
   */
  data class ServiceCardClick(val profileId: String, val resourceId: String) : ViewComponentEvent()
}
