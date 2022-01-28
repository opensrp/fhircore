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
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.json.JSONArray
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.APP_SYNC_CONFIG
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.ID
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.ORGANIZATION
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.PUBLISHER
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import timber.log.Timber

class QuestConfigService
@Inject
constructor(
  @ApplicationContext val context: Context,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ConfigService {
  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }
  override val resourceSyncParams: Map<ResourceType, Map<String, String>> by lazy {
    val searchParams = loadSearchParams(context)
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    for (i in searchParams.indices) {
      // TODO: expressionValue supports for Organization and Publisher, extend it using
      // Composition resource
      val expressionValue =
        searchParams[i].name!!.let {
          when {
            it.contains(ORGANIZATION) -> authenticatedUserInfo?.organization
            it.contains(PUBLISHER) -> authenticatedUserInfo?.questionnairePublisher
            it.contains(ID) -> searchParams[i].expression
            else -> null
          }
        }

      pairs.add(
        Pair(
          ResourceType.fromCode(searchParams[i].base[0].code),
          expressionValue?.let { mapOf(searchParams[i].code to it) } ?: mapOf()
        )
      )
    }

    Timber.i("SYNC CONFIG $pairs")

    mapOf(*pairs.toTypedArray())
  }

  private fun loadSearchParams(context: Context): List<SearchParameter> {
    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val json = context.assets.open(APP_SYNC_CONFIG).bufferedReader().use { it.readText() }
    val searchParameters = mutableListOf<SearchParameter>()

    val jsonArrayEntry = JSONArray(json)
    (0 until jsonArrayEntry.length()).forEach {
      searchParameters.add(
        iParser.parseResource(jsonArrayEntry.getJSONObject(it).toString()) as SearchParameter
      )
    }
    return searchParameters
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
