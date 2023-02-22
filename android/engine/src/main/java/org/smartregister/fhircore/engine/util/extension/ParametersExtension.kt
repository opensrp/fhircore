package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource

fun Parameters.addResourceParameter(name: String, resource: Resource) = this.addParameter(
    Parameters.ParametersParameterComponent().apply {
        this.name = name
        this.resource = resource
    }

)