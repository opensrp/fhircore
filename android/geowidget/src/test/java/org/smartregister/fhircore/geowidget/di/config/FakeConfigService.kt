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
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.ResourceTag

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

  override fun defineResourceTags() =
    listOf(
      ResourceTag(
        type = ResourceType.CareTeam.name,
        tag =
          Coding().apply {
            system = CARETEAM_SYSTEM
            display = CARETEAM_DISPLAY
          }
      ),
      ResourceTag(
        type = ResourceType.Location.name,
        tag =
          Coding().apply {
            system = LOCATION_SYSTEM
            display = LOCATION_DISPLAY
          }
      ),
      ResourceTag(
        type = ResourceType.Organization.name,
        tag =
          Coding().apply {
            system = ORGANIZATION_SYSTEM
            display = ORGANIZATION_DISPLAY
          }
      ),
      ResourceTag(
        type = ResourceType.Practitioner.name,
        tag =
          Coding().apply {
            system = PRACTITIONER_SYSTEM
            display = PRACTITIONER_DISPLAY
          }
      )
    )

  override fun provideConfigurationSyncPageSize(): String {
    return "100"
  }

  companion object {
    const val CARETEAM_SYSTEM = "http://fake.tag.com/CareTeam#system"
    const val CARETEAM_DISPLAY = "Practitioner CareTeam"
    const val ORGANIZATION_SYSTEM = "http://fake.tag.com/Organization#system"
    const val ORGANIZATION_DISPLAY = "Practitioner Organization"
    const val LOCATION_SYSTEM = "http://fake.tag.com/Location#system"
    const val LOCATION_DISPLAY = "Practitioner Location"
    const val PRACTITIONER_SYSTEM = "http://fake.tag.com/Practitioner#system"
    const val PRACTITIONER_DISPLAY = "Practitioner"
  }
}
