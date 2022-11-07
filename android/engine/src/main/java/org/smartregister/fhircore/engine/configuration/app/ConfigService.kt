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

package org.smartregister.fhircore.engine.configuration.app

import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.sync.SyncStrategyTag
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

/** An interface that provides the application configurations. */
interface ConfigService {

  /** Provide [AuthConfiguration] for the application */
  fun provideAuthConfiguration(): AuthConfiguration

  /**
   * [SyncStrategyTag] defines whether to sync resource based on the IDs of CareTeam, Location,
   * Organization and Practitioner. Each SyncStrategy represents a meta tag that is used by all
   * synced resource.
   */
  fun provideSyncStrategyTags(): List<SyncStrategyTag>

  fun provideSyncStrategies(): List<String>

  fun provideMandatorySyncTags(sharedPreferencesHelper: SharedPreferencesHelper): List<Coding> {
    val syncStrategies = provideSyncStrategies()
    val tags = mutableListOf<Coding>()
    provideSyncStrategyTags().forEach { strategy ->
      if (syncStrategies.contains(strategy.type)) {
        if (strategy.type == SharedPreferenceKey.PRACTITIONER_ID.name) {
          sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_ID.name, null)?.let { id ->
            strategy.tag?.let { tags.add(it.copy().apply { code = id.extractLogicalIdUuid() }) }
          }
        } else {
          sharedPreferencesHelper.read<List<String>>(strategy.type)?.forEach { id ->
            strategy.tag?.let { tags.add(it.copy().apply { code = id.extractLogicalIdUuid() }) }
          }
        }
      }
    }

    return tags
  }

  fun provideConfigurationSyncPageSize(): String
}
