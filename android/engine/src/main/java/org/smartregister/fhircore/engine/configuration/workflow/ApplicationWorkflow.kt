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

package org.smartregister.fhircore.engine.domain.model

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
  LAUNCH_PROFILE
}
