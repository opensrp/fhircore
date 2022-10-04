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

package org.smartregister.fhircore.quest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.SyncStrategy

@Singleton
class QuestConfigService @Inject constructor(@ApplicationContext val context: Context) :
  ConfigService {

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      clientId = BuildConfig.OAUTH_CIENT_ID,
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
      accountType = context.getString(R.string.authenticator_account_type)
    )

  override fun provideSyncStrategy() =
    SyncStrategy().apply {
      careTeamTag.tag =
        Coding().apply {
          system = context.getString(R.string.sync_strategy_careteam_system)
          display = context.getString(R.string.sync_strategy_careteam_display)
        }
      locationTag.tag =
        Coding().apply {
          system = context.getString(R.string.sync_strategy_location_system)
          display = context.getString(R.string.sync_strategy_location_display)
        }
      organizationTag.tag =
        Coding().apply {
          system = context.getString(R.string.sync_strategy_organization_system)
          display = context.getString(R.string.sync_strategy_organization_display)
        }
      practitionerTag.tag =
        Coding().apply {
          system = context.getString(R.string.sync_strategy_practitioner_system)
          display = context.getString(R.string.sync_strategy_practitioner_display)
        }
    }
}
