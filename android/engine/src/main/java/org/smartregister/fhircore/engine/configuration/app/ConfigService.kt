/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import org.hl7.fhir.r4.model.ResourceType
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

  /**
   * Provide a list of [Coding] that represents [ResourceTag]. [Coding] can be directly appended to
   * a FHIR resource.
   */
  fun provideResourceTags(sharedPreferencesHelper: SharedPreferencesHelper): List<Coding> {
    val tags = mutableListOf<Coding>()
    defineResourceTags().forEach { strategy ->
      if (strategy.type == ResourceType.Practitioner.name) {
        val id = sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
        if (id.isNullOrBlank()) {
          strategy.tag.let { tag -> tags.add(tag.copy().apply { code = "Not defined" }) }
        } else {
          strategy.tag.let { tag ->
            tags.add(tag.copy().apply { code = id.extractLogicalIdUuid() })
          }
        }
      } else {
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

    return tags
  }

  fun provideConfigurationSyncPageSize(): String
}
