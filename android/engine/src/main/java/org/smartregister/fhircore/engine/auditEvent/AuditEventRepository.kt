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

package org.smartregister.fhircore.engine.auditEvent

import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.AuditEvent
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Practitioner
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference

@Singleton
class AuditEventRepository
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val sharedPreferences: SharedPreferencesHelper,
) : IAuditEventRepository {
  override suspend fun createAuditEvent() {
    // Get Practitioner Resource
    val practitionerID =
      sharedPreferences.read(key = SharedPreferenceKey.PRACTITIONER_ID.name, defaultValue = null)
        ?: return

    val practitioner = defaultRepository.loadResource<Practitioner>(practitionerID!!)

    val context = sharedPreferences.context

    // Create AuditEvent Resource
    val auditEvent =
      AuditEvent().apply {
        id = UUID.randomUUID().toString()
        type =
          Coding().apply {
            system = context.getString(R.string.audit_event_system)
            code = "110114"
            display = "User Authentication"
          }
        subtype =
          listOf(
            Coding().apply {
              system = context.getString(R.string.audit_event_system)
              code = "110122"
              display = "Login"
            },
          )
        outcome = AuditEvent.AuditEventOutcome._0
        action = AuditEvent.AuditEventAction.C
        recorded = Date()
        agent =
          listOf(
            AuditEvent.AuditEventAgentComponent().apply {
              who = practitioner?.asReference()
              requestor = true
            },
          )
        source =
          AuditEvent.AuditEventSourceComponent().apply { observer = practitioner?.asReference() }
      }

    // Save AuditEvent Resource
    defaultRepository.addOrUpdate(true, auditEvent)
  }
}

interface IAuditEventRepository {
  suspend fun createAuditEvent()
}
