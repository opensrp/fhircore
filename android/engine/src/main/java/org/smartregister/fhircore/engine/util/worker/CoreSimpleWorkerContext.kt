package org.smartregister.fhircore.engine.util.worker

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource

class CoreSimpleWorkerContext : SimpleWorkerContext() {
    private val hapi =
        HapiWorkerContext(
            FhirContext.forR4Cached(),
            FhirContext.forR4Cached().validationSupport,
        )

    init {
        setExpansionProfile(Parameters())
        isCanRunWithoutTerminology = true
    }

    override fun <T : Resource?> fetchResourceWithException(theClass: Class<T>?, uri: String?): T? {
        if (uri == null) {
            return null
        }
        if (uri.startsWith("https://terminology.hl7.org") || uri.startsWith("http://snomed.info/sct") || uri.startsWith(
                "https://d-tree.org"
            )
        ) {
            return null
        }
        return hapi.fetchResourceWithException(theClass, uri)
            ?: super.fetchResourceWithException(theClass, uri)
    }

}