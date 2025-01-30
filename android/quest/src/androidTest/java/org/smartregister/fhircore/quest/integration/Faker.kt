/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.LocalChange
import com.google.android.fhir.SearchResult
import com.google.android.fhir.db.LocalChangeResourceReference
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.upload.SyncUploadProgress
import com.google.android.fhir.sync.upload.UploadRequestResult
import com.google.android.fhir.sync.upload.UploadStrategy
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltTestApplication
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.sync.ResourceTag
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

object Faker {

  private const val APP_DEBUG = "app/debug"

  fun buildTestConfigurationRegistry(): ConfigurationRegistry {
    val fhirEngine =
      object : FhirEngine {
        override suspend fun clearDatabase() {}

        override suspend fun count(search: Search): Long = 0

        override suspend fun create(vararg resource: Resource, isLocalOnly: Boolean): List<String> =
          emptyList()

        override suspend fun delete(type: ResourceType, id: String) {}

        override suspend fun get(type: ResourceType, id: String): Resource {
          return when (type) {
            ResourceType.Composition -> Composition()
            ResourceType.Bundle -> Bundle()
            ResourceType.Patient -> Patient()
            else -> TODO("Not yet implemented")
          }
        }

        override suspend fun getLastSyncTimeStamp(): OffsetDateTime? = OffsetDateTime.now()

        override suspend fun getLocalChanges(type: ResourceType, id: String): List<LocalChange> =
          emptyList()

        override suspend fun getUnsyncedLocalChanges(): List<LocalChange> = emptyList()

        override suspend fun purge(type: ResourceType, id: String, forcePurge: Boolean) {}

        override suspend fun purge(
          type: ResourceType,
          ids: Set<String>,
          forcePurge: Boolean,
        ) {}

        override suspend fun <R : Resource> search(search: Search): List<SearchResult<R>> =
          emptyList()

        override suspend fun syncDownload(
          conflictResolver: ConflictResolver,
          download: suspend () -> Flow<List<Resource>>,
        ) {
          download().collect {}
        }

        override suspend fun syncUpload(
          uploadStrategy: UploadStrategy,
          upload:
            suspend (List<LocalChange>, List<LocalChangeResourceReference>) -> Flow<
                UploadRequestResult,
              >,
        ): Flow<SyncUploadProgress> {
          return flowOf()
        }

        override suspend fun update(vararg resource: Resource) {}

        override suspend fun withTransaction(block: suspend FhirEngine.() -> Unit) {
          TODO("Not yet implemented")
        }
      }

    val fhirResourceService =
      object : FhirResourceService {
        override suspend fun getResource(url: String) = Bundle()

        override suspend fun getResourceWithGatewayModeHeader(
          fhirGatewayMode: String?,
          url: String,
        ): Bundle {
          TODO("Not yet implemented")
        }

        override suspend fun insertResource(
          resourceType: String,
          id: String,
          body: RequestBody,
        ): Resource {
          TODO("Not yet implemented")
        }

        override suspend fun updateResource(
          resourceType: String,
          id: String,
          body: RequestBody,
        ): OperationOutcome {
          TODO("Not yet implemented")
        }

        override suspend fun deleteResource(resourceType: String, id: String): OperationOutcome {
          TODO("Not yet implemented")
        }

        override suspend fun fetchImage(url: String): ResponseBody? {
          TODO("Not yet implemented")
        }

        override suspend fun searchResource(
          resourceType: String,
          searchParameters: Map<String, String>,
        ): Bundle {
          TODO("Not yet implemented")
        }

        override suspend fun post(url: String, body: RequestBody): Bundle {
          TODO("Not yet implemented")
        }
      }

    val configService =
      object : ConfigService {
        override fun provideAuthConfiguration(): AuthConfiguration {
          return AuthConfiguration(
            fhirServerBaseUrl = "http://fake.base.url.com",
            oauthServerBaseUrl = "http://fake.keycloak.url.com",
            clientId = "fake-client-id",
            accountType = InstrumentationRegistry.getInstrumentation().context.packageName,
          )
        }

        override fun defineResourceTags(): List<ResourceTag> {
          return listOf(
            ResourceTag(
              type = ResourceType.Location.name,
              tag =
                Coding().apply {
                  system = "http://fake.tag.com/Location#system"
                  display = "Practitioner Location"
                },
            ),
          )
        }

        override fun getLauncherIcon(): Int = 0
      }

    val json = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
      isLenient = true
      useAlternativeNames = true
    }

    val configurationRegistry =
      ConfigurationRegistry(
        fhirEngine = fhirEngine,
        fhirResourceDataSource = FhirResourceDataSource(fhirResourceService),
        sharedPreferencesHelper =
          SharedPreferencesHelper(ApplicationProvider.getApplicationContext(), gson = Gson()),
        configService = configService,
        dispatcherProvider = DefaultDispatcherProvider(),
        json = json,
        context = ApplicationProvider.getApplicationContext<HiltTestApplication>(),
      )

    runBlocking {
      configurationRegistry.loadConfigurations(
        appId = APP_DEBUG,
        context = ApplicationProvider.getApplicationContext(),
      ) {}
    }

    return configurationRegistry
  }
}
