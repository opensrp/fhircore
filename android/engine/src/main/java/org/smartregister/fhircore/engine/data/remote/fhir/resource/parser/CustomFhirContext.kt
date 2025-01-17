/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.data.remote.fhir.resource.parser

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.parser.IParserErrorHandler
import ca.uhn.fhir.parser.LenientErrorHandler

class CustomFhirContext : FhirContext(FhirVersionEnum.R4) {
  private val myParserErrorHandler: IParserErrorHandler = LenientErrorHandler()

  override fun newJsonParser(): IParser {
    return CustomJsonParser(
      this,
      myParserErrorHandler,
    )
  }
}
