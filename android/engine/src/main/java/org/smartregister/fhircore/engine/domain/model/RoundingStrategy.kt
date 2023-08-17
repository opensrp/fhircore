package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.json.JsonNames
import java.math.RoundingMode

/**
 * Represents different types of rounding strategies that can be applied to Decimal numbers within
 * the application
 */
@Suppress("EXPLICIT_SERIALIZABLE_IS_REQUIRED")
enum class RoundingStrategy(val value: RoundingMode) {
    @JsonNames("truncate", "TRUNCATE")
    TRUNCATE(RoundingMode.DOWN),

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