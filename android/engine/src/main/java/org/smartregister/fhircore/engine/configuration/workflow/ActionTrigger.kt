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

package org.smartregister.fhircore.engine.configuration.workflow

/** An application event that can trigger a workflow. Examples button click, on back press etc */
enum class ActionTrigger {
  /**
   * An action that is performed when user presses a button or any actionable component in the UI
   */
  ON_CLICK,

  /** Action that is triggered to count register items */
  ON_COUNT
}
