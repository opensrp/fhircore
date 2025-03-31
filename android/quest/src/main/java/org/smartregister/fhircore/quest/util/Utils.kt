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
   * Removes any '#' characters that appear before the first string or integer value.
   *
   * @param resourceId The string or integer value that may have multiple '#' characters at the
   *   start
   * @return The value without any '#' characters at the start
   */
  fun removeHashPrefix(resourceId: Any): String {
    val stringValue =
      when (resourceId) {
        is String -> resourceId
        is Int -> resourceId.toString()
        else -> resourceId.toString()
      }
    return stringValue.trimStart('#')
  }
}
