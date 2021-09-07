/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.anc.shadow

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.model.CarePlan

object TestUtils {
  private val iParser: IParser = FhirContext.forR4().newJsonParser()

  fun loadCarePlan(context: Context, questionnaire: String): CarePlan {
    val qJson = context.assets.open(questionnaire).bufferedReader().use { it.readText() }
    return iParser.parseResource(qJson) as CarePlan
  }
}
