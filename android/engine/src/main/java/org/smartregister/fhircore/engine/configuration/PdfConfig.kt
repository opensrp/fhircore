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

package org.smartregister.fhircore.engine.configuration

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
@Parcelize
data class PdfConfig(
  val pdfTitle: String? = null,
  val pdfTitleSuffix: String? = null,
  val pdfStructureReference: String? = null,
  val subjectReference: String? = null,
  val subjectType: ResourceType? = null,
  val questionnaireReferences: List<String> = emptyList()
) : java.io.Serializable, Parcelable {

  fun interpolate(computedValuesMap: Map<String, Any>) =
    this.copy(
      pdfTitle = pdfTitle?.interpolate(computedValuesMap),
      pdfStructureReference = pdfStructureReference?.interpolate(computedValuesMap),
      subjectReference = subjectReference?.interpolate(computedValuesMap),
      questionnaireReferences = questionnaireReferences.map { it.interpolate(computedValuesMap) },
    )
}
