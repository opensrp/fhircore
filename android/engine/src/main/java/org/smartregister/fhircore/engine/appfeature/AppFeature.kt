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

package org.smartregister.fhircore.engine.appfeature

/**
 * A representation of application features. An application feature can have a list of supported
 * actions. Actions can be triggered via a click action on the side menu or clicking a menu item
 * from the toolbar.
 */
sealed class AppFeature(val name: String) {
  object InAppReporting : AppFeature(name = "InAppReporting")
  object PatientManagement : AppFeature(name = "PatientManagement")
  object HouseholdManagement : AppFeature(name = "HouseholdManagement")
  object DeviceToDeviceSync : AppFeature(name = "DeviceToDeviceSync")
}
