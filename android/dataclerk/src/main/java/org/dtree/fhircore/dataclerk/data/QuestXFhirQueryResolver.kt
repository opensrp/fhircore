package org.dtree.fhircore.dataclerk.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestXFhirQueryResolver @Inject constructor(val fhirEngine: FhirEngine) : XFhirQueryResolver {
    override suspend fun resolve(xFhirQuery: String): List<Resource> {
        return fhirEngine.search(xFhirQuery)
    }
}
