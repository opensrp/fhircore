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

package org.smartregister.fhircore.quest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.ResourceTag

@Singleton
class QuestConfigService @Inject constructor(@ApplicationContext val context: Context) :
  ConfigService {

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      clientId = BuildConfig.OAUTH_CLIENT_ID,
      accountType = BuildConfig.APPLICATION_ID,
    )

  override fun defineResourceTags() =
    listOf(
      ResourceTag(
        type = ResourceType.CareTeam.name,
        tag =
          Coding().apply {
            system =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_careteam_system,
              )
            display =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_careteam_display,
              )
          },
      ),
      ResourceTag(
        type = ResourceType.Location.name,
        tag =
          Coding().apply {
            system =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_location_system,
              )
            display =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_location_display,
              )
          },
      ),
      ResourceTag(
        type = ResourceType.Organization.name,
        tag =
          Coding().apply {
            system =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_organization_system,
              )
            display =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_organization_display,
              )
          },
      ),
      ResourceTag(
        type = ResourceType.Practitioner.name,
        tag =
          Coding().apply {
            system =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_practitioner_system,
              )
            display =
              context.getString(
                org.smartregister.fhircore.engine.R.string.sync_strategy_practitioner_display,
              )
          },
      ),
      ResourceTag(
        type = APP_VERSION,
        tag =
          Coding().apply {
            system = context.getString(R.string.app_version_tag_url)
            code = BuildConfig.VERSION_NAME
            display = context.getString(R.string.application_version)
          },
      ),
    )

  override fun provideConfigurationSyncPageSize(): String {
    return BuildConfig.CONFIGURATION_SYNC_PAGE_SIZE
  }

  companion object {
    private const val APP_VERSION = "AppVersion"
  }
}
