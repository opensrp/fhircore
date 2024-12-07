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

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.sync.ResourceTag
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

/** An interface that provides the application configurations. */
interface ConfigService {

  /** Provide [AuthConfiguration] for the application. */
  fun provideAuthConfiguration(): AuthConfiguration

  /** Define a list of [ResourceTag] for the application. */
  fun defineResourceTags(): List<ResourceTag>

  /** Return the App's launcher icon for use in notifications */
  fun getLauncherIcon(): Int

  /**
   * Provide a list of [Coding] that represents [ResourceTag]. [Coding] can be directly appended to
   * a FHIR resource.
   */
  fun provideResourceTags(sharedPreferencesHelper: SharedPreferencesHelper): List<Coding> {
    val tags = mutableListOf<Coding>()
    defineResourceTags().forEach { strategy ->
      when (strategy.type) {
        ResourceType.Practitioner.name -> {
          val id = sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
          if (id.isNullOrBlank() || id.isEmpty()) {
            strategy.tag.let { tag -> tags.add(tag.copy().apply { code = "Not defined" }) }
          } else {
            strategy.tag.let { tag ->
              tags.add(tag.copy().apply { code = id.extractLogicalIdUuid() })
            }
          }
        }
        APP_VERSION -> tags.add(strategy.tag.copy())
        else -> {
          val ids = sharedPreferencesHelper.read<List<String>>(strategy.type)
          if (ids.isNullOrEmpty()) {
            strategy.tag.let { tag -> tags.add(tag.copy().apply { code = "Not defined" }) }
          } else {
            ids.forEach { id ->
              strategy.tag.let { tag ->
                tags.add(tag.copy().apply { code = id.extractLogicalIdUuid() })
              }
            }
          }
        }
      }
    }
    return tags
  }

  /**
   * Provide a list of custom search parameters.
   *
   * @return list of predefined custom search parameters.
   */
  fun provideCustomSearchParameters(): List<SearchParameter> {
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
    const val ACTIVE_SEARCH_PARAM = "active"
    const val APP_VERSION = "AppVersion"
    const val STATUS_SEARCH_PARAM = "status"
    const val SORT_SEARCH_PARAM = "sort"
    const val SEARCH_PARAM = "search"
    const val MEDICATION_SORT_URL = "http://smartregister.org/SearchParameter/medication-sort"
  }
}
