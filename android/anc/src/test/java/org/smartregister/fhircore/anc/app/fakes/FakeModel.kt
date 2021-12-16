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

package org.smartregister.fhircore.anc.app.fakes

import java.text.SimpleDateFormat
import java.util.Date
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType

object FakeModel {

  fun buildCarePlan(subject: String): CarePlan {
    return CarePlan().apply {
      this.subject = Reference().apply { reference = "Patient/$subject" }
      this.addActivity().detail.apply {
        this.scheduledPeriod.start = Date()
        this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    }
  }

  fun getEncounter(patientId: String): Encounter {
    return Encounter().apply {
      id = "1"
      type = listOf(getCodeableConcept())
      subject = Reference().apply { reference = "Patient/$patientId" }
      status = Encounter.EncounterStatus.FINISHED
      class_ = Coding("", "", "ABC")
      period = Period().apply { start = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01") }
    }
  }

  fun getCodeableConcept(): CodeableConcept {
    return CodeableConcept().apply {
      id = "1"
      coding = listOf(getCodingList())
      text = "ABC"
    }
  }

  fun getCodingList(): Coding {
    return Coding().apply {
      id = "1"
      system = "123"
      code = "123"
      display = "ABC"
    }
  }

  fun buildPatient(id: String, family: String, given: String): Patient {
    return Patient().apply {
      this.id = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }

  fun getObservation(testValue: Int = 4) =
    Observation().apply {
      value = IntegerType(testValue)
      effective = DateTimeType.now()
    }

  fun getObservationQuantity(testValue: Double = 4.0) =
    Observation().apply {
      value = Quantity(testValue)
      effective = DateTimeType.now()
    }
}
