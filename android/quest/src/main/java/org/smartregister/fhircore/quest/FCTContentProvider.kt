package org.smartregister.fhircore.quest

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        Timber.d("--method: $method --arg: $arg --extras: $extras")

        if (method == "db_operation") {

            val queryRequest = arg!!.decompress().decodeJson<QueryRequest>()

            val dbHelper = DBHelper(context, queryRequest.database, getDatabaseVersion())
            val db = dbHelper.writableDatabase

            val query = queryRequest.query
            val data = when(getQueryType(query)) {
                QueryType.SELECT, QueryType.UNKNOWN -> {

                    var cursor: Cursor? = null
                    var result = ""
                    try {
                        cursor = db.rawQuery(query, null)
                        result = extractAllRecords(cursor, queryRequest)
                    } catch (ex: Exception) {
                        val jsonObject = JSONObject().apply {
                            put("success", false)
                            put("error", ex.localizedMessage ?: ex.message ?: ex.toString())
                        }
                        result = jsonObject.toString()
                    } finally {
                        cursor?.close()
                    }
                    result
                }
                else -> {
                    val jsonObject = JSONObject()
                    try {
                        db.execSQL(query)
                        jsonObject.put("success", true)
                    } catch (ex: Exception) {
                        jsonObject.put("success", false)
                        jsonObject.put("error", ex.localizedMessage ?: ex.message ?: ex.toString())
                    }
                    jsonObject.toString()
                }
            }
           // val cursor = db.rawQuery(arg?.replace("+", " "), null)

            //DatabaseUtils.dumpCursor(cursor)

            //cursor.close()

            return Bundle().apply {
                putString("data", data.compress())
            }
        }

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

    private fun getQueryType(query: String) : QueryType {
        return when {
            isFound(query, "^SELECT\\s+") -> QueryType.SELECT
            isFound(query, "^INSERT\\s+") -> QueryType.INSERT
            isFound(query, "^UPDATE\\s+") -> QueryType.UPDATE
            isFound(query, "^DELETE\\s+") -> QueryType.DELETE
            else -> QueryType.UNKNOWN
        }
    }

    private fun isFound(query: String, pattern: String): Boolean {
        return pattern.toRegex(RegexOption.IGNORE_CASE).find(query.trim()) != null
    }

    private fun extractAllRecords(cursor: Cursor, queryRequest: QueryRequest): String {
        val jsonObject = JSONObject()
        val jsonArray = JSONArray()

        jsonObject.put("count", cursor.count)

        if (cursor.moveToPosition(queryRequest.offset)) {
            do {
                val obj = JSONObject()
                cursor.columnNames.forEachIndexed { columnIndex, columName ->
                    when (cursor.getType(columnIndex)) {
                        Cursor.FIELD_TYPE_BLOB -> {
                            obj.put(columName, cursor.getBlob(columnIndex).toByteString().hex())
                        }
                        Cursor.FIELD_TYPE_INTEGER -> {
                            obj.put(columName, cursor.getInt(columnIndex))
                        }
                        Cursor.FIELD_TYPE_FLOAT -> {
                            obj.put(columName, cursor.getFloat(columnIndex))
                        }
                        else -> {
                            obj.put(columName, cursor.getString(columnIndex))
                        }
                    }
                }
                jsonArray.put(obj)
            } while (cursor.position < ((queryRequest.offset - 1) + queryRequest.limit) && cursor.moveToNext())
        }

        val columnNameJsonArray = JSONArray()
        cursor.columnNames.forEach {
            columnNameJsonArray.put(it)
        }

        jsonObject.put("success", true)
        jsonObject.put("data", jsonArray)
        jsonObject.put("columnNames", columnNameJsonArray)
        return jsonObject.toString()
    }

    private fun getDatabaseVersion(): Int {
        val fhirDatabase = fhirEngine.javaClass.getDeclaredField("database").apply { isAccessible = true }.get(fhirEngine)
        val resourceDatabaseImpl = fhirDatabase.javaClass.getDeclaredField("db").apply { isAccessible = true }.get(fhirDatabase)
        val sqliteOpenHelper = resourceDatabaseImpl.javaClass.getMethod("getOpenHelper").invoke(resourceDatabaseImpl)
        val supportSqliteDatabase = sqliteOpenHelper.javaClass.getMethod("getReadableDatabase").invoke(sqliteOpenHelper)
        val dbVersion = supportSqliteDatabase.javaClass.getDeclaredMethod("getVersion").invoke(supportSqliteDatabase)
        return dbVersion as Int
    }

    fun String.compress(): String {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).bufferedWriter().use { it.write(this) }
        return Base64.getEncoder().encodeToString(byteStream.toByteArray())
    }

    fun String.decompress(): String {
        val compressedBytes = Base64.getDecoder().decode(this)
        val byteArrayInputStream = ByteArrayInputStream(compressedBytes)
        return GZIPInputStream(byteArrayInputStream).bufferedReader().use { it.readText() }
    }

    class DBHelper(context: Context?, database: String, version: Int) : SQLiteOpenHelper(context, database, null, version) {
        override fun onCreate(db: SQLiteDatabase?) {}

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    }

    enum class QueryType {
        SELECT, INSERT, UPDATE, DELETE, UNKNOWN
    }

    @Serializable
    data class QueryRequest(
        val database: String,
        val query: String,
        val sortColumn: String? = null,
        val offset: Int,
        val limit: Int,
    )
}