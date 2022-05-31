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
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Narrative
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo

object FakeModel {

  const val NUMERATOR = "numerator"
  const val DENOMINATOR = "denominator"

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

  fun getMeasureReport(typeMR: MeasureReport.MeasureReportType): MeasureReport {
    return MeasureReport().apply {
      id = "12333"
      status = MeasureReport.MeasureReportStatus.COMPLETE
      type = typeMR
      addGroup().apply {
        id = "222"
        addStratifier().apply {
          id = "123"
          addStratum().apply {
            id = "1234"
            addPopulation().apply {
              id = NUMERATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(2)
            }
            addPopulation().apply {
              id = DENOMINATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(3)
            }
            value =
              CodeableConcept().apply {
                id = "123"
                coding = arrayListOf(Coding("hh", "hh", "hh"), Coding("", "hh2", "hh2"))
              }
          }
        }
      }
    }
  }

  fun getMeasureReportWithoutValue(): MeasureReport {
    return MeasureReport().apply {
      id = "12333"
      status = MeasureReport.MeasureReportStatus.COMPLETE
      addGroup().apply {
        id = "222"
        addStratifier().apply {
          id = "123"
          addStratum().apply {
            id = "1234"
            addPopulation().apply {
              id = NUMERATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(2)
            }
            addPopulation().apply {
              id = DENOMINATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(3)
            }
            value =
              CodeableConcept().apply {
                id = "123"
                coding = arrayListOf()
              }
          }
        }
      }
    }
  }

  fun getMeasureReportWithText(): MeasureReport {
    return MeasureReport().apply {
      id = "12333"
      status = MeasureReport.MeasureReportStatus.COMPLETE
      addGroup().apply {
        id = "222"
        addStratifier().apply {
          id = "123"
          text = Narrative().apply { status = Narrative.NarrativeStatus.GENERATED }
          addStratum().apply {
            id = "1234"
            addPopulation().apply {
              id = NUMERATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(0)
            }
            addPopulation().apply {
              id = DENOMINATOR
              MeasureReport.StratifierGroupPopulationComponent().countElement = IntegerType(0)
            }
            text = Narrative().apply { status = Narrative.NarrativeStatus.GENERATED }
          }
        }
      }
    }
  }

  fun getUserInfo(): UserInfo {
    val userInfo =
      UserInfo().apply {
        questionnairePublisher = "ab"
        organization = "1111"
        keycloakUuid = "123"
      }
    return userInfo
  }
}
