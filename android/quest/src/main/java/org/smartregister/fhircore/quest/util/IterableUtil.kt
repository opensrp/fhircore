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

/**
 * Helper to return a defaultValue when getting maybe null keys from a map.
 *
 * @param A the type of the map key
 * @param B the type of the map value
 * @property map the map to get from
 * @property key the key that may be null
 * @property defaultValue a default value that may not be null and defaults to an empty string
 */
fun <A, B> nonNullGetOrDefault(map: Map<A, B>, key: A?, defaultValue: B): B {
  return if (key != null) map.getOrDefault(key, defaultValue) else defaultValue
}
