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

package org.dtree.fhircore.dataclerk

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.ResourceTag
import org.smartregister.fhircore.engine.util.SharedPreferenceKey

@Singleton
class DataClerkConfigService @Inject constructor(@ApplicationContext val context: Context) :
  ConfigService {

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      clientId = BuildConfig.OAUTH_CIENT_ID,
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
      accountType = BuildConfig.APPLICATION_ID
    )

  override fun defineResourceTags() =
    listOf(
      ResourceTag(
        type = ResourceType.CareTeam.name,
        tag =
          Coding().apply {
            system = context.getString(R.string.sync_strategy_careteam_system)
            display = context.getString(R.string.sync_strategy_careteam_display)
          }
      ),
      ResourceTag(
        type = ResourceType.Location.name,
        tag =
          Coding().apply {
            system = context.getString(R.string.sync_strategy_location_system)
            display = context.getString(R.string.sync_strategy_location_display)
          }
      ),
      ResourceTag(
        type = ResourceType.Organization.name,
        tag =
          Coding().apply {
            system = context.getString(R.string.sync_strategy_organization_system)
            display = context.getString(R.string.sync_strategy_organization_display)
          }
      ),
      ResourceTag(
        type = SharedPreferenceKey.PRACTITIONER_ID.name,
        tag =
          Coding().apply {
            system = context.getString(R.string.sync_strategy_practitioner_system)
            display = context.getString(R.string.sync_strategy_practitioner_display)
          },
        isResource = false
      ),
      ResourceTag(
        type = SharedPreferenceKey.APP_ID.name,
        tag =
          Coding().apply {
            system = context.getString(R.string.sync_strategy_appid_system)
            display = context.getString(R.string.application_id)
          },
        isResource = false
      )
    )
}
