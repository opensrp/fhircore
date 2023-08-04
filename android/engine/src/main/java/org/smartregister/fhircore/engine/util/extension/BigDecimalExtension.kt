package org.smartregister.fhircore.engine.util.extension

import org.smartregister.fhircore.engine.domain.model.RoundingStrategy
import java.math.BigDecimal


fun BigDecimal?.rounding(roundingStrategy: RoundingStrategy, roundingPrecision: Int): String {
    return this?.setScale(roundingPrecision, roundingStrategy.value)?.toString() ?: "0"
}
