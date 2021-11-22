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

package org.smartregister.fhircore.quest.robolectric

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SyncDownloadContext
import com.google.android.fhir.db.impl.dao.LocalChangeToken
import com.google.android.fhir.db.impl.dao.SquashedLocalChange
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.time.OffsetDateTime
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.quest.QuestApplication

class QuestTestApplication : QuestApplication() {

  override val syncJob: SyncJob
    get() = spyk(Sync.basicSyncJob(ApplicationProvider.getApplicationContext()))

  override var applicationConfiguration: ApplicationConfiguration = applicationConfigurationOf()

  override val authenticationService: AuthenticationService
    get() = spyk(FhirAuthenticationService())

  override val fhirEngine: FhirEngine by lazy { spyk(FhirEngineImpl()) }

  override val secureSharedPreference: SecureSharedPreference by lazy {
    val secureSharedPreferenceSpy =
      spyk(SecureSharedPreference(ApplicationProvider.getApplicationContext()))
    every { secureSharedPreferenceSpy.retrieveCredentials() } returns
      AuthCredentials(
        username = "demo",
        password = "Amani123",
        refreshToken = "",
        sessionToken = "same-gibberish-string-as-token"
      )
    secureSharedPreferenceSpy
  }

  override var workerContextProvider: SimpleWorkerContext =
    mockk(relaxed = true) { SimpleWorkerContext() }

  override fun schedulePeriodicSync() {
    // Do nothing
  }

  override fun onCreate() {
    super.onCreate()
    configurationRegistry.loadAppConfigurations("quest", this) {
      // Do nothing
    }
  }

  inner class FhirEngineImpl : FhirEngine {

    val mockedResourcesStore = mutableListOf<Resource>()

    override suspend fun count(search: Search): Long = 1

    override suspend fun getLastSyncTimeStamp(): OffsetDateTime? = OffsetDateTime.now()

    override suspend fun <R : Resource> load(clazz: Class<R>, id: String): R {
      val existingResource =
        mockedResourcesStore.find { it.hasId() && it.id == id }
          ?: throw ResourceNotFoundException(id)
      return existingResource as R
    }

    override suspend fun <R : Resource> remove(clazz: Class<R>, id: String) {
      mockedResourcesStore.removeIf { it.id == id }
    }

    override suspend fun <R : Resource> save(vararg resource: R) {
      mockedResourcesStore.addAll(resource)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Resource> search(search: Search): List<R> =
      mockedResourcesStore.filter { search.filter(TokenClientParam(it.id), Identifier()) } as
        List<R>

    override suspend fun syncDownload(download: suspend (SyncDownloadContext) -> List<Resource>) {
      // Do nothing
    }

    override suspend fun syncUpload(
      upload: suspend (List<SquashedLocalChange>) -> List<LocalChangeToken>
    ) {
      // Do nothing
    }

    override suspend fun <R : Resource> update(resource: R) {
      // Replace old resource
      mockedResourcesStore.removeIf { it.hasId() && it.id == resource.id }
      mockedResourcesStore.add(resource)
    }
  }

  inner class FhirAuthenticationService :
    AuthenticationService(ApplicationProvider.getApplicationContext()) {
    override fun skipLogin(): Boolean = false

    override fun getLoginActivityClass(): Class<*> = LoginActivity::class.java

    override fun getAccountType(): String = "test.account.type"

    override fun clientSecret(): String = "test.client.secret"

    override fun clientId(): String = "test.client.id"

    override fun providerScope(): String = "openid"

    override fun getApplicationConfigurations(): ApplicationConfiguration = applicationConfiguration
  }
}
