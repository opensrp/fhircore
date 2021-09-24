package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Reference

fun Reference.extractId(): String {
    return this.reference.split("/").last()
}