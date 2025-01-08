package org.smartregister.fhircore.engine.data.remote.fhir.resource.parser

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.parser.IParserErrorHandler
import ca.uhn.fhir.parser.LenientErrorHandler


class CustomFhirContext: FhirContext(FhirVersionEnum.R4) {
    private val myParserErrorHandler: IParserErrorHandler = LenientErrorHandler()
    override fun newJsonParser(): IParser {
        return CustomJsonParser(
            this,
            myParserErrorHandler
        )
    }

}