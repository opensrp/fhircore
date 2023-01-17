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

package org.smartregister.fhircore.engine

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule

fun ComposeContentTestRule.waitUntilNodeCount(
  matcher: SemanticsMatcher,
  count: Int = 1,
  timeoutMillis: Long = 5_000L
) {
  this.waitUntil(timeoutMillis) { this.onAllNodes(matcher).fetchSemanticsNodes().size == count }
}

fun ComposeContentTestRule.waitUntilExists(
  matcher: SemanticsMatcher,
  timeoutMillis: Long = 5_000L
) {
  return this.waitUntilNodeCount(matcher, 1, timeoutMillis)
}

fun ComposeContentTestRule.waitUntilDoesNotExist(
  matcher: SemanticsMatcher,
  timeoutMillis: Long = 5_000L
) {
  return this.waitUntilNodeCount(matcher, 0, timeoutMillis)
}
