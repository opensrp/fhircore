package org.smartregister.fhircore.quest

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.paging.liveData
import ca.uhn.fhir.context.FhirContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.ui.register.RegisterViewModel
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.internal.wait
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.quest.ui.register.RegisterFilterState
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

class FCTProvider : ContentProvider() {
    //private val registerViewModel by viewModels<RegisterViewModel>()

    private lateinit var registerViewModel: RegisterViewModel
    private val parser = FhirContext.forR4Cached().newJsonParser()
   /* @Inject
    lateinit var registerRepository: RegisterRepository
    @Inject
    lateinit var configurationRegistry: ConfigurationRegistry
    @Inject
    lateinit var sharedPreferenceHelper: SharedPreferencesHelper
    @Inject lateinit var dispatcherProvider: DispatcherProvider
    @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor
*/
    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface FCTProviderEntryPoint {
        fun registerRepository(): RegisterRepository
        fun configurationRegistry(): ConfigurationRegistry
        fun sharedPreferenceHelper(): SharedPreferencesHelper
        fun dispatcherProvider(): DispatcherProvider
        fun resourceDataRulesExecutor(): ResourceDataRulesExecutor
    }

    private fun getRegisterRepository(appContext: Context): RegisterRepository {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.registerRepository()
    }

    private fun getConfigurationRegistry(appContext: Context): ConfigurationRegistry {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.configurationRegistry()
    }

    private fun getSharedPreferencesHelper(appContext: Context): SharedPreferencesHelper {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.sharedPreferenceHelper()
    }

    private fun getDispatcherProvider(appContext: Context): DispatcherProvider {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.dispatcherProvider()
    }

    private fun getResourceDataRulesExecutor(appContext: Context): ResourceDataRulesExecutor {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.resourceDataRulesExecutor()
    }

    override fun onCreate(): Boolean {

        registerViewModel = RegisterViewModel(
            registerRepository = getRegisterRepository(context!!),
            configurationRegistry = getConfigurationRegistry(context!!),
            sharedPreferencesHelper = getSharedPreferencesHelper(context!!),
            dispatcherProvider = getDispatcherProvider(context!!),
            resourceDataRulesExecutor = getResourceDataRulesExecutor(context!!)
        )
        return true
    }

    override fun query(uri: Uri,projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        Timber.d("arg: $arg")
        val result = runBlocking {
            this.async {

                /*getRegisterRepository(context!!).loadRegisterData(
                    currentPage = 0,
                    registerId = arg!!,
                    fhirResourceConfig = null,
                )*/

                val compressedBytes = Base64.getDecoder().decode(arg!!)
                val byteArrayInputStream = ByteArrayInputStream(compressedBytes)
                val configJson = GZIPInputStream(byteArrayInputStream)
                    .bufferedReader()
                    .use { it.readText() }

                val registerConfiguration = getConfigurationRegistry(context!!).transformConfiguration<RegisterConfiguration>(configJson)

                val data = getRegisterRepository(context!!).searchResourcesRecursively(
                    filterByRelatedEntityLocationMetaTag = registerConfiguration.filterDataByRelatedEntityLocation,
                    filterActiveResources = registerConfiguration.activeResourceFilters,
                    fhirResourceConfig = registerConfiguration.fhirResource,
                    secondaryResourceConfigs = registerConfiguration.secondaryResources,
                    currentPage = 0,
                    pageSize = registerConfiguration.pageSize,
                    configRules = registerConfiguration.configRules,
                )

                val rulesList = mutableListOf<Map<String, Any>>()
                data.forEach {
                    val rules = getResourceDataRulesExecutor(context!!).computeResourceDataRules(
                        ruleConfigs = registerConfiguration.registerCard.rules,
                        repositoryResourceData = it,
                        params = emptyMap()
                    )
                    rulesList.add(rules)
                }


                Pair(rulesList, data)
            }
        }.getCompleted()


        return Bundle().apply {
            val out = StringBuilder()
            out.appendLine()
            result.second.forEachIndexed() { index, item ->

                out.appendLine("resource: " + parser.encodeResourceToString(item.resource))
                out.appendLine("relatedResourcesMap: " + item.relatedResourcesMap.toString())
                out.appendLine("relatedResourcesCountMap: " + item.relatedResourcesCountMap.toString())
                out.appendLine("rules: " + result.first[index])
                out.appendLine()
            }

            putString("data", out.toString())
        }

    }
}