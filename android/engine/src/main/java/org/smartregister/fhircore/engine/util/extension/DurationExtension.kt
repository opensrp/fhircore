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
import java.time.format.DateTimeParseException
import kotlin.time.Duration as KotlinDuration

/**
 * Parses a string that represents a duration in ISO-8601 format and returns the parsed Duration
 * value. If parsing fails a default of 1 day duration value is returned
 */
fun KotlinDuration.Companion.tryParse(durationString: String): Duration {
  return try {
    Duration.parse(durationString)
  } catch (ex: DateTimeParseException) {
    return Duration.ofDays(1)
  }
}
