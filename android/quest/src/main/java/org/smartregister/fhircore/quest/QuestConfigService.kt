package org.smartregister.fhircore.quest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService

class QuestConfigService @Inject constructor(@ApplicationContext val context: Context) :
  ConfigService {

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      return mapOf(
        ResourceType.CarePlan to mapOf(),
        ResourceType.Patient to mapOf(),
        ResourceType.Questionnaire to mapOf(),
        ResourceType.QuestionnaireResponse to mapOf(),
        ResourceType.Binary to mapOf()
      )
    }

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      clientId = BuildConfig.OAUTH_CIENT_ID,
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
      accountType = context.getString(R.string.authenticator_account_type)
    )
}
