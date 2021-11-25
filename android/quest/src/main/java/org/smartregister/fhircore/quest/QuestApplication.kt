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

import android.app.Application
import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.json.JSONArray
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_QUESTIONNAIRE_PUBLISHER_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.join
import org.smartregister.fhircore.engine.util.extension.runPeriodicSync
import timber.log.Timber

open class QuestApplication : Application(), ConfigurableApplication {

  override val syncJob: SyncJob
    get() = Sync.basicSyncJob(getContext())

  override lateinit var applicationConfiguration: ApplicationConfiguration
  val SAMPLE_SEARCH_PARAMETER_PATIENT_FILE = "sample_search_parameter_patient.json"
  val SYNC_BY_TAGS = "sync_by_tags.json"

  override val authenticationService: AuthenticationService
    get() = QuestAuthenticationService(applicationContext)

  override val fhirEngine: FhirEngine by lazy { FhirEngineProvider.getInstance(this) }

  override val fhirPathEngine = FHIRPathEngine(SimpleWorkerContext())

  override val secureSharedPreference: SecureSharedPreference
    get() = SecureSharedPreference(applicationContext)


/*  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
  get() {
    return mapOf(
            ResourceType.CarePlan to mapOf(),
            ResourceType.Patient to mapOf(),
            ResourceType.Questionnaire to mapOf(),
            ResourceType.QuestionnaireResponse to mapOf(),
            ResourceType.Binary to mapOf()
    )
  }*/

  /*override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      val searchParams = loadSearchParams(this)

      val practitionerRole: PractitionerRole = PractitionerRole().apply {
        this.organization = Reference().apply {
          this.reference = "Organization/3454"
        }
      }

      val organization = fhirPathEngine.evaluate(practitionerRole, "organization")

      val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
      for (i in searchParams.indices) {
        pairs.add(Pair(ResourceType.fromCode(searchParams[i].base[0].code), mapOf(searchParams[i].expression to (organization.first() as Reference).reference)))
      }

      return mapOf(*pairs.toTypedArray())
    }*/

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() {
      val searchParams = loadSearchParams(this)

      val practitionerRole: PractitionerRole = PractitionerRole().apply {
        this.meta.tag = listOf(Coding("https://www.snomed.org", "35359004", "Tag-code"))
      }

      val tag = fhirPathEngine.evaluate(practitionerRole, "meta.tag")

      val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
      for (i in searchParams.indices) {
        pairs.add(Pair(ResourceType.fromCode(searchParams[i].base[0].code), mapOf(searchParams[i].expression to (tag.first() as Coding).code)))
      }

      return mapOf(*pairs.toTypedArray())
    }


  private fun loadSearchParams(context: Context): List<SearchParameter> {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val json = context.assets.open(SYNC_BY_TAGS)
            .bufferedReader().use { it.readText() }
    val searchParameters = mutableListOf<SearchParameter>()

    val jsonArrayEntry = JSONArray(json)
    for (i in 0 until jsonArrayEntry.length()) {
      searchParameters.add(iParser.parseResource(jsonArrayEntry[i].toString()) as SearchParameter)
    }
    return searchParameters
  }

  private fun buildPublisherFilterMap(): MutableMap<String, String> {
    val questionnaireFilterMap: MutableMap<String, String> = HashMap()
    val publisher = getPublisher()
    if (publisher != null) questionnaireFilterMap[Questionnaire.SP_PUBLISHER] = publisher
    return questionnaireFilterMap
  }

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration
    this.applicationConfiguration.apply {
      fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL
      oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL
      clientId = BuildConfig.OAUTH_CIENT_ID
      clientSecret = BuildConfig.OAUTH_CLIENT_SECRET
    }
    SharedPreferencesHelper.write(SharedPreferencesHelper.THEME, applicationConfiguration.theme)
  }

  override fun schedulePeriodicSync() {
    this.runPeriodicSync<QuestFhirSyncWorker>()
  }

  override fun onCreate() {
    super.onCreate()
    SharedPreferencesHelper.init(this)
    questApplication = this

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
    DataCaptureConfig.attachmentResolver = ReferenceAttachmentResolver(this)
  }

  companion object {
    private lateinit var questApplication: QuestApplication
    private const val CONFIG_PROFILE = "quest-app-profile"

    fun getProfileConfigId() = CONFIG_PROFILE.join(getPublisher()?.lowercase()?.let { "-$it" }, "")

    fun getContext() = questApplication

    fun getPublisher() =
      SharedPreferencesHelper.read(USER_QUESTIONNAIRE_PUBLISHER_SHARED_PREFERENCE_KEY, null)
  }
}
