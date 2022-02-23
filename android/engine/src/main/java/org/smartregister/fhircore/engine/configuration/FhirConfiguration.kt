package org.smartregister.fhircore.engine.configuration

import org.hl7.fhir.r4.model.Resource

data class FhirConfiguration<T: Resource>(
    override val appId: String,
    override val classification: String,
    val resource: T
): Configuration