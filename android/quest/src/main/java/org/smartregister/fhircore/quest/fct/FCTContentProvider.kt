package org.smartregister.fhircore.quest.fct

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import androidx.core.database.getBlobOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.ui.register.RegisterViewModel
import timber.log.Timber
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.util.extension.decodeJson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class FCTContentProvider : ContentProvider() {
    //private val registerViewModel by viewModels<RegisterViewModel>()

    private lateinit var registerViewModel: RegisterViewModel
    private val parser = FhirContext.forR4Cached().newJsonParser()
    private lateinit var fhirEngine: FhirEngine
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
        fun getFhirEngine(): FhirEngine
        fun registerRepository(): RegisterRepository
        fun configurationRegistry(): ConfigurationRegistry
        fun sharedPreferenceHelper(): SharedPreferencesHelper
        fun dispatcherProvider(): DispatcherProvider
        fun resourceDataRulesExecutor(): ResourceDataRulesExecutor
    }

    private fun getFhirEngine(appContext: Context): FhirEngine {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FCTProviderEntryPoint::class.java
        )
        return hiltEntryPoint.getFhirEngine()
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
            //dispatcherProvider = getDispatcherProvider(context!!),
            resourceDataRulesExecutor = getResourceDataRulesExecutor(context!!)
        )
        fhirEngine = getFhirEngine(context!!)
        return true
    }

    override fun query(uri: Uri,projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        Timber.d("--method: $method --arg: $arg --extras: $extras")
        val decompressArg = arg!!.decompress()

        return if (method == DB_OPERATION) {

            val dbBridge = DatabaseBridge(context!!, fhirEngine)
            val result = dbBridge.execute(decompressArg)
            Bundle().apply {
                putString(DATA, result.compress())
            }

        } else {
            Bundle()
        }
    }

    private fun String.compress(): String {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).bufferedWriter().use { it.write(this) }
        return Base64.getEncoder().encodeToString(byteStream.toByteArray())
    }

    private fun String.decompress(): String {
        val compressedBytes = Base64.getDecoder().decode(this)
        val byteArrayInputStream = ByteArrayInputStream(compressedBytes)
        return GZIPInputStream(byteArrayInputStream).bufferedReader().use { it.readText() }
    }

    companion object {
        const val DATA = "data"
        const val DB_OPERATION = "db_operation"
    }
}