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

package org.smartregister.fhircore.quest.util

/** Utility object containing helper functions for common operations. */
object Utils {
  /**
   * Removes the '#' prefix from a resource ID string if present.
   *
   * @param resourceIds The resource ID string that may or may not have a '#' prefix
   * @return The resource ID string without the '#' prefix
   */
  fun removeHashPrefix(resourceIds: String): String {
    return if (resourceIds.startsWith("#")) resourceIds.substring(1) else resourceIds
  }
}
