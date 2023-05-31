package org.smartregister.fhircore.quest.util

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.TypeDetails
import org.hl7.fhir.r4.model.ValueSet
import org.hl7.fhir.r4.utils.FHIRPathEngine.IEvaluationContext

class FhirpathConstantResolver: IEvaluationContext {

    override fun resolveConstant(data: Any, name: String, beforeContext: Boolean): Base? {
        return (data as? Map<*, *>)?.get(name) as? Base
    }

    override fun resolveConstantType(p0: Any?, p1: String?): TypeDetails {
        TODO("Not yet implemented")
    }

    override fun log(p0: String?, p1: MutableList<Base>?): Boolean {
        TODO("Not yet implemented")
    }

    override fun resolveFunction(p0: String?): IEvaluationContext.FunctionDetails {
        TODO("Not yet implemented")
    }

    override fun checkFunction(p0: Any?, p1: String?, p2: MutableList<TypeDetails>?): TypeDetails {
        TODO("Not yet implemented")
    }

    override fun executeFunction(
        p0: Any?,
        p1: MutableList<Base>?,
        p2: String?,
        p3: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        TODO("Not yet implemented")
    }

    override fun resolveReference(p0: Any?, p1: String?): Base {
        TODO("Not yet implemented")
    }

    override fun conformsToProfile(p0: Any?, p1: Base?, p2: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun resolveValueSet(p0: Any?, p1: String?): ValueSet {
        TODO("Not yet implemented")
    }
}