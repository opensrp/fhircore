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
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.configuration.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.ID
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.ORGANIZATION
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.PUBLISHER
import org.smartregister.fhircore.engine.configuration.FhirConfiguration
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import timber.log.Timber
import java.lang.reflect.Parameter
import javax.inject.Singleton

@Singleton
class QuestConfigService
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
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    val syncConfig = loadSyncParams()

    // TODO Does not support nested parameters i.e. parameters.parameters...
    // TODO: expressionValue supports for Organization and Publisher literals for now
    syncConfig.resource.parameter.map { it.resource as SearchParameter }.forEach { sp ->
      val paramName = sp.name!! // e.g. organization
      val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
      val paramExpression = sp.expression
      val expressionValue =
        when (paramName) {
          ORGANIZATION -> authenticatedUserInfo?.organization
          PUBLISHER -> authenticatedUserInfo?.questionnairePublisher
          ID -> paramExpression
          else -> null
        }?.let {
          // replace the evaluated value into expression for complex expressions
          // e.g. #organization -> 123
          // e.g. patient.organization eq #organization -> patient.organization eq 123
          paramExpression.replace(paramLiteral, it)
        }

      // for each entity in base create and add param map
      // [Patient=[ name=Abc, organization=111 ], Encounter=[ type=MyType, location=MyHospital ],..]
      sp.base
        .map { base ->
          Pair(
            ResourceType.fromCode(base.code),
            expressionValue?.let { mapOf(sp.code to it) } ?: mapOf()
          )
        }
        .run { pairs.addAll(this) }
    }

    Timber.i("SYNC CONFIG $pairs")

    mapOf(*pairs.toTypedArray())
  }

  fun loadSyncParams(): FhirConfiguration<Parameters> =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.SYNC)

  override fun provideAuthConfiguration() =
    AuthConfiguration(
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
      clientId = BuildConfig.OAUTH_CIENT_ID,
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
      accountType = context.getString(R.string.authenticator_account_type)
    )
}
