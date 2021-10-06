package org.smartregister.fhircore.engine.impl

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SyncDownloadContext
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.db.impl.dao.LocalChangeToken
import com.google.android.fhir.db.impl.dao.SquashedLocalChange
import com.google.android.fhir.search.ReferenceFilter
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJob
import io.mockk.spyk
import java.time.OffsetDateTime
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.shadow.ShadowNpmPackageProvider
import org.smartregister.fhircore.engine.shadow.activity.ShadowLoginActivity
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@Config(shadows = [ShadowNpmPackageProvider::class])
class FhirApplication : Application(), ConfigurableApplication {

  private val dataMap = mutableMapOf<String, MutableList<Resource>>()

  override val syncJob: SyncJob
    get() = Sync.basicSyncJob(ApplicationProvider.getApplicationContext())

  override var applicationConfiguration: ApplicationConfiguration =
    applicationConfigurationOf()

  override val authenticationService: AuthenticationService
    get() = spyk(FhirAuthenticationService())

  override val fhirEngine: FhirEngine
    get() = spyk(FhirEngineImpl())

  override val secureSharedPreference: SecureSharedPreference
    get() = spyk(SecureSharedPreference(ApplicationProvider.getApplicationContext()))

  override val resourceSyncParams: Map<ResourceType, Map<String, String>>
    get() = mapOf()

  override val workerContextProvider: SimpleWorkerContext
    get() = spyk(SimpleWorkerContext())

  override fun configureApplication(applicationConfiguration: ApplicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration
  }

  override fun schedulePeriodicSync() {
    // Do nothing
  }

  private fun <T> getListOfFilters(search: Search, filterName: String): MutableList<T> {
    val field = search.javaClass.getDeclaredField(filterName)
    field.isAccessible = true
    return field.get(search) as MutableList<T>
  }

  inner class FhirEngineImpl : FhirEngine {
    override suspend fun count(search: Search): Long {
      return -1
    }

    override suspend fun getLastSyncTimeStamp(): OffsetDateTime? {
      return OffsetDateTime.now()
    }

    override suspend fun <R : Resource> load(clazz: Class<R>, id: String): R {
      if (dataMap.containsKey(id)) {
        return dataMap[id]?.first() as R
      } else {
        throw ResourceNotFoundException(clazz.name, id)
      }
    }

    override suspend fun <R : Resource> remove(clazz: Class<R>, id: String) {
      if (dataMap.containsKey(id)) {
        if (dataMap[id]!!.isNotEmpty()) {
          dataMap[id]!!.removeLast()
        }

        if (dataMap[id]!!.isEmpty()) {
          dataMap.remove(id)
        }
      }
    }

    override suspend fun <R : Resource> save(vararg resource: R) {
      resource.forEach {
        if (it.id != null) {
          if (dataMap.containsKey(it.id)) {
            dataMap[it.id]?.add(it)
          } else {
            dataMap[it.id] = mutableListOf(it)
          }
        }
      }
    }

    override suspend fun <R : Resource> search(search: Search): List<R> {
      val referenceFilter = getListOfFilters<ReferenceFilter>(search, "referenceFilters")

      val result = mutableListOf<Resource>()

      if (referenceFilter.isNotEmpty()) {
        dataMap.forEach {
          if (it.key == referenceFilter.first().value) {
            result.addAll(it.value)
          }
        }
      }

      return result as List<R>
    }

    override suspend fun syncDownload(download: suspend (SyncDownloadContext) -> List<Resource>) {}

    override suspend fun syncUpload(
      upload: suspend (List<SquashedLocalChange>) -> List<LocalChangeToken>
    ) {}

    override suspend fun <R : Resource> update(resource: R) {
      dataMap.forEach {
        if (it.key == resource.id) {
          if (it.value.isNotEmpty()) {
            it.value[0] = resource
          }
        }
      }
    }
  }

  inner class FhirAuthenticationService : AuthenticationService(ApplicationProvider.getApplicationContext()) {
    override fun skipLogin(): Boolean = false

    override fun getLoginActivityClass(): Class<*> = ShadowLoginActivity::class.java

    override fun getAccountType(): String = "test.account.type"

    override fun clientSecret(): String = "test.client.secret"

    override fun clientId(): String = "test.client.id"

    override fun providerScope(): String = "openid"

    override fun getApplicationConfigurations(): ApplicationConfiguration = applicationConfiguration
  }
}
