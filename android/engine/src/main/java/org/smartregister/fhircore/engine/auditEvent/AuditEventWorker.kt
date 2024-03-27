package org.smartregister.fhircore.engine.auditEvent

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.AuditEvent
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Practitioner
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import timber.log.Timber
import java.util.Date
import java.util.UUID

@HiltWorker
class AuditEventWorker
@AssistedInject
constructor(
    @Assisted val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    val fhirEngine: FhirEngine,
    val sharedPreferences: SharedPreferencesHelper,
    val defaultRepository: DefaultRepository
    ) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        Timber.e("AuditEventWorker is running")
        createAuditEvent()
        return Result.success()
    }

    private suspend fun createAuditEvent(){
        // Get Practitioner Resource
        val practitionerID =
            sharedPreferences.read(key = SharedPreferenceKey.PRACTITIONER_ID.name, defaultValue = null)
        val practitioner = defaultRepository.loadResource<Practitioner>(practitionerID!!)

        // Create AuditEvent Resource
        val auditEvent = AuditEvent().apply {
            id = UUID.randomUUID().toString()
            type = Coding().apply {
                system = "http://dicom.nema.org/resources/ontology/DCM"
                code ="110114"
                display = "User Authentication"
            }
            subtype = listOf(
                Coding().apply {
                    system = "http://dicom.nema.org/resources/ontology/DCM"
                    code = "110122"
                    display = "Login"
                }
            )
            outcome = AuditEvent.AuditEventOutcome._0
            action = AuditEvent.AuditEventAction.C
            recorded = Date()
            agent = listOf(
                AuditEvent.AuditEventAgentComponent().apply {
                    who = practitioner?.asReference()
                    requestor = true
                }
            )
            source = AuditEvent.AuditEventSourceComponent().apply {
                site = "https://d-tree.org"
                observer = practitioner?.asReference()
            }
        }

        // Save AuditEvent Resource
        defaultRepository.addOrUpdate(true, auditEvent,)
    }

    companion object{
        const val NAME = "AuditEventWorker"
    }

}