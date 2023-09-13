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

package org.smartregister.fhircore.engine.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.testing.jsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.auth.KeycloakService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.practitionerEndpointUrl
import org.smartregister.model.practitioner.PractitionerDetails
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

@HiltViewModel
class DevViewModel
@Inject
constructor(
    val syncBroadcaster: SyncBroadcaster,
    val accountAuthenticator: AccountAuthenticator,
    val secureSharedPreference: SecureSharedPreference,
    val sharedPreferencesHelper: SharedPreferencesHelper,
    val configurationRegistry: ConfigurationRegistry,
    val keycloakService: KeycloakService,
    val fhirResourceService: FhirResourceService,
    val fhirEngine: FhirEngine,
) : ViewModel() {

    suspend fun createResourceReport(context: Context) {
        try {
            generateReport(context)
            val file = File(context.filesDir, "log_data.txt")
            val fileUri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(shareIntent, "Share Log Data")

            if (shareIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun generateReport(context: Context) {

        val questionnaire = fhirEngine.search<Questionnaire> { }
        val structureMaps = fhirEngine.search<StructureMap> { }

        val logs = "Id,Questionnaire,Date\n" +
                questionnaire.joinToString(separator = "\n") { "${it.logicalId},${it.meta.versionId},${it.meta.lastUpdated.asDdMmmYyyy()}" } +
                "\n-----------------------------------------------------" +
                "" +
                "Id,StructureMap,Data\n" +
                structureMaps.joinToString(separator = "\n") { "${it.logicalId},${it.meta.versionId},${it.meta.lastUpdated.asDdMmmYyyy()}" }

        val fileName = "log_data.txt"
        val file = File(context.filesDir, fileName)
        val fileWriter = FileWriter(file)
        fileWriter.write(logs)
        fileWriter.close()

    }

    fun fetchDetails() {
        try {
            viewModelScope.launch {
                val userInfo = keycloakService.fetchUserInfo().body()
                if (userInfo != null && !userInfo.keycloakUuid.isNullOrEmpty()) {
                    val bundle =
                        fhirResourceService.getResource(url = userInfo.keycloakUuid!!.practitionerEndpointUrl())
                    val practitionerDetails = bundle.entry.first().resource as PractitionerDetails

                    val data = jsonParser.encodeResourceToString(practitionerDetails)
                    println(data)
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }
}
