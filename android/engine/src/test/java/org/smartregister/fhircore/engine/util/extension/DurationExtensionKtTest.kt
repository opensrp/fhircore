/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util.extension

import java.time.Duration
import kotlin.time.Duration as KotlinDuration
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class DurationExtensionKtTest : RobolectricTest() {

  @Test
  fun `parsing an ISO-8601 format string returns the correct duration`() {
    val durationString = "PT02H"
    Assert.assertEquals(Duration.ofHours(2), KotlinDuration.tryParse(durationString))
  }

  @Test
  fun `parsing a wrong ISO-8601 format string returns the default duration of 1 day`() {
    val durationString = "PTH2"
    Assert.assertEquals(Duration.ofDays(1), KotlinDuration.tryParse(durationString))
  }
}
