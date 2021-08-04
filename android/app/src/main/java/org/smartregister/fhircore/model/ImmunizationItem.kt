package org.smartregister.fhircore.model

data class ImmunizationItem(
    val vaccine: String,
    val doses: List<Pair<String, Int>>
)
