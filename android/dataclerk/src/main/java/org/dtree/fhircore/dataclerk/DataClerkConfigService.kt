package org.dtree.fhircore.dataclerk

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import javax.inject.Inject
import javax.inject.Singleton

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
}
