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

package org.smartregister.fhircore.engine.configuration.app

import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.extensions.logicalId
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractType

class CustomSearchParameterService(
  val storageDir: File,
  val iParser: IParser,
  val dispatcherProvider: DispatcherProvider,
) {

  fun getCustomSearchParameters(): Iterable<SearchParameter> {
    return predefinedCustomSearchParameters
  }

  fun readSavedSearchParameter(): Iterable<SearchParameter> {
    // Since we might have to use it synchronously when initializing FhirEngine, maybe look into how
    // to convert suspend function to normal java callback
    // https://github.com/google/dagger/issues/1502
  }

  suspend fun saveBundle(bundle: Bundle) {
    val fileName = "${bundle.logicalId.ifBlank { UUID.randomUUID().toString() }}.json"
    val file = File(storageDir, fileName)
    withContext(dispatcherProvider.io()) {
      FileOutputStream(file).use { it.write(iParser.encodeResourceToString(bundle).toByteArray()) }
    }
  }

  /** List of predefined custom search parameters. */
  private val predefinedCustomSearchParameters: List<SearchParameter>
    get() {
      val activeGroupSearchParameter =
        SearchParameter().apply {
          url = "http://smartregister.org/SearchParameter/group-active"
          addBase("Group")
          name = ACTIVE_SEARCH_PARAM
          code = ACTIVE_SEARCH_PARAM
          type = Enumerations.SearchParamType.TOKEN
          expression = "Group.active"
          description = "Search the active field"
        }

      val flagStatusSearchParameter =
        SearchParameter().apply {
          url = "http://smartregister.org/SearchParameter/flag-status"
          addBase("Flag")
          name = STATUS_SEARCH_PARAM
          code = STATUS_SEARCH_PARAM
          type = Enumerations.SearchParamType.TOKEN
          expression = "Flag.status"
          description = "Search the status field"
        }

      val medicationSortSearchParameter =
        SearchParameter().apply {
          url = MEDICATION_SORT_URL
          addBase("Medication")
          name = SORT_SEARCH_PARAM
          code = SORT_SEARCH_PARAM
          type = Enumerations.SearchParamType.NUMBER
          expression = "Medication.extension.where(url = '$MEDICATION_SORT_URL').value"
          description = "Search the sort field"
        }

      val patientSearchParameter =
        SearchParameter().apply {
          url = "http://smartregister.org/SearchParameter/patient-search"
          addBase("Patient")
          name = SEARCH_PARAM
          code = SEARCH_PARAM
          type = Enumerations.SearchParamType.STRING
          expression = "Patient.name.text | Patient.identifier.value"
          description = "Search patients by name and identifier fields"
        }

      return listOf(
        activeGroupSearchParameter,
        flagStatusSearchParameter,
        medicationSortSearchParameter,
        patientSearchParameter,
      )
    }

  companion object {
    private const val ACTIVE_SEARCH_PARAM = "active"
    private const val STATUS_SEARCH_PARAM = "status"
    private const val SORT_SEARCH_PARAM = "sort"
    private const val SEARCH_PARAM = "search"
    private const val MEDICATION_SORT_URL =
      "http://smartregister.org/SearchParameter/medication-sort"
  }
}

fun isCompositionSectionSearchParameter(section: Composition.SectionComponent): Boolean =
  section.code.coding.any {
    it.system.lowercase() == SEARCH_PARAMETER_SECTION_SYSTEM_URL &&
      it.code.lowercase() == SEARCH_PARAMETER_SECTION_CODE
  }

fun isSearchParameterConfigReferenceValid(reference: Reference): Boolean =
  reference.extractType() == ResourceType.Bundle

private const val SEARCH_PARAMETER_SECTION_SYSTEM_URL =
  "http://smartregister.org/CodeSystem/composition-section-codes"
private const val SEARCH_PARAMETER_SECTION_CODE = "custom-search-parameter-bundle"
