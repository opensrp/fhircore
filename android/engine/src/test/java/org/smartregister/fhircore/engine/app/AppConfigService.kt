package org.smartregister.fhircore.engine.app

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService

class AppConfigService @Inject constructor(@ApplicationContext val context: Context) :
  ConfigService {

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() =
      mapOf(
        ResourceType.Patient to emptyMap(),
        ResourceType.Immunization to emptyMap(),
        ResourceType.Questionnaire to emptyMap(),
      )

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = "http://fake.base.url.com",
      oauthServerBaseUrl = "http://fake.keycloak.url.com",
      clientId = "fake-client-id",
      clientSecret = "siri-fake",
      accountType = context.packageName
    )
}
