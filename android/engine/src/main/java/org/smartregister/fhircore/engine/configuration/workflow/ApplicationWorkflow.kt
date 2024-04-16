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

package org.smartregister.fhircore.engine.configuration.workflow

enum class ApplicationWorkflow {

  /**
   * A workflow for starting a register. This workflow prompts the app to load a list of clients
   * filtered as configured.
   */
  LAUNCH_REGISTER,

  /**
   * A workflow for starting a profile. This workflow prompts the app to load data for a patient as
   * configured.
   */
  LAUNCH_PROFILE,

  /** A workflow for navigating to in-app reports */
  LAUNCH_REPORT,

  /** A workflow for navigating to user's settings screen */
  LAUNCH_SETTINGS,

  /** A workflow to trigger device to device sync */
  DEVICE_TO_DEVICE_SYNC,

  /** A workflow that opens a new questionnaire form or an existing questionnaire for editing */
  LAUNCH_QUESTIONNAIRE,

  /** A workflow for navigating to maps */
  LAUNCH_MAP,

  /** A workflow for changing a managing entity */
  CHANGE_MANAGING_ENTITY,

  /** A workflow that launches the dialer with the phone number ready to place a call */
  LAUNCH_DIALLER,

  /** A workflow that launches user insight screen */
  LAUNCH_INSIGHT_SCREEN,

  /** A workflow that copies text to keyboard */
  COPY_TEXT,
}
