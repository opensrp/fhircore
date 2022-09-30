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

package org.smartregister.fhircore.geowidget.di.config

import javax.inject.Inject
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.SyncStrategy

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 22-08-2022. */
class FakeConfigService @Inject constructor() : ConfigService {
  override fun provideAuthConfiguration(): AuthConfiguration {
    return AuthConfiguration(
      fhirServerBaseUrl = "https://fhir-server.com",
      oauthServerBaseUrl = "https://fhir-oauth-server.com/oauth",
      clientId = "client-id",
      clientSecret = "client-secret",
      accountType = "account-type"
    )
  }

  override fun provideSyncStrategy() =
    SyncStrategy().apply {
      careTeamTag.tag =
        Coding().apply {
          system = "http://fake.tag.com/CareTeam#system"
          display = "Practitioner CareTeam"
        }
      locationTag.tag =
        Coding().apply {
          system = "http://fake.tag.com/Location#system"
          display = "Practitioner Location"
        }
      organizationTag.tag =
        Coding().apply {
          system = "http://fake.tag.com/Organization#system"
          display = "Practitioner Organization"
        }
      practitionerTag.tag =
        Coding().apply {
          system = "http://fake.tag.com/Practitioner#system"
          display = "Practitioner"
        }
    }
}
