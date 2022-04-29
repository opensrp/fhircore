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

package org.smartregister.fhircore.mwcore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson

@Singleton
class MwCoreConfigService
@Inject
constructor(
    @ApplicationContext val context: Context,
    val sharedPreferencesHelper: SharedPreferencesHelper,
    val configurationRegistry: ConfigurationRegistry
) : ConfigService {
    private val authenticatedUserInfo by lazy {
        sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
    }
    override val resourceSyncParams: Map<ResourceType, Map<String, String>> by lazy {
        loadRegistrySyncParams(configurationRegistry, authenticatedUserInfo)
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
