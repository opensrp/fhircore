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

package org.smartregister.fhircore.engine.domain.model

import java.math.RoundingMode
import kotlinx.serialization.json.JsonNames

/**
 * Represents different types of rounding strategies that can be applied to Decimal numbers within
 * the application
 */
@Suppress("EXPLICIT_SERIALIZABLE_IS_REQUIRED")
enum class RoundingStrategy(val value: RoundingMode) {
  @JsonNames("truncate", "TRUNCATE") TRUNCATE(RoundingMode.DOWN),
  @JsonNames(
    "round_up",
    "roundUp",
    "ROUND_UP",
  )
  ROUND_UP(RoundingMode.UP),
  @JsonNames(
    "round_off",
    "roundOff",
    "ROUND_OFF",
  )
  ROUND_OFF(RoundingMode.HALF_UP)
}
