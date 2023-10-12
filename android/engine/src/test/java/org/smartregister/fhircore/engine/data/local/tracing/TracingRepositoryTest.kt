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

package org.smartregister.fhircore.engine.data.local.tracing

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.domain.model.TracingHistory
import org.smartregister.fhircore.engine.domain.model.TracingOutcome
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.referenceValue

@OptIn(ExperimentalCoroutinesApi::class)
class TracingRepositoryTest {

  private val fhirEngine = mockk<FhirEngine>()
  private lateinit var tracingRepository: TracingRepository

  @Before
  fun setUp() {
    tracingRepository = TracingRepository(fhirEngine)
  }

  @Test
  fun getTracingHistoryReturnsExpected() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "little-john", 1, patientType = "exposed-infant")
    val task0 =
      Task().apply {
        id = "task0"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "miss-routine"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val enc0 =
      Encounter().apply {
        id = "enc0"
        status = Encounter.EncounterStatus.FINISHED
        class_ =
          Coding().apply {
            system = "https://d-tree.org"
            code = "IMP"
            display = "inpatient encounter"
          }
        serviceType =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = "home-tracing-outcome"
            }
          )
        subject = patient0.asReference()
        period = Period().apply { start = Date() }
        addReasonCode().apply {
          addCoding().apply {
            system = "https://d-tree.org"
            code = "miss-routine"
            display = "Missed Routine"
          }
          text = "Missed Routine"
        }
      }
    val list0 =
      ListResource().apply {
        id = "list0"
        title = "Tracing Encounter List_1"
        orderedBy =
          CodeableConcept(
              Coding().apply {
                system = "https://d-tree.org"
                code = "1"
              }
            )
            .apply { text = "1" }
        mode = ListResource.ListMode.SNAPSHOT
        status = ListResource.ListStatus.CURRENT
        subject = patient0.asReference()
        date = Date()
        addEntry().apply {
          item = task0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-task"
                }
              )
              .apply { text = task0.referenceValue() }
        }
        addEntry().apply {
          item = enc0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-enc"
                }
              )
              .apply { text = enc0.referenceValue() }
        }
      }

    coEvery { fhirEngine.get(ResourceType.Task, task0.logicalId) } returns task0
    coEvery { fhirEngine.get(ResourceType.Encounter, enc0.logicalId) } returns enc0
    coEvery {
      fhirEngine.search<ListResource>(
        Search(ResourceType.List, from = 0, count = PaginationConstant.DEFAULT_PAGE_SIZE)
      )
    } returns listOf(SearchResult(list0, included = null, revIncluded = null))

    val tracingHistory =
      tracingRepository
        .getTracingHistory(0, loadAll = false, patientId = patient0.logicalId)
        .single()
    Assert.assertEquals(
      TracingHistory(
        historyId = list0.logicalId,
        startDate = list0.date,
        endDate = null,
        numberOfAttempts = 1,
        isActive = true
      ),
      tracingHistory
    )
  }

  @Test
  fun getTracingOutcomesReturnsExpected() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "little-john", 1, patientType = "exposed-infant")
    val task0 =
      Task().apply {
        id = "task0"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "miss-routine"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val enc0 =
      Encounter().apply {
        id = "enc0"
        status = Encounter.EncounterStatus.FINISHED
        class_ =
          Coding().apply {
            system = "https://d-tree.org"
            code = "IMP"
            display = "inpatient encounter"
          }
        serviceType =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = "home-tracing-outcome"
            }
          )
        subject = patient0.asReference()
        period = Period().apply { start = Date() }
        addReasonCode().apply {
          addCoding().apply {
            system = "https://d-tree.org"
            code = "miss-routine"
            display = "Missed Routine"
          }
          text = "Missed Routine"
        }
      }
    val list0 =
      ListResource().apply {
        id = "list0"
        title = "Tracing Encounter List_1"
        orderedBy =
          CodeableConcept(
              Coding().apply {
                system = "https://d-tree.org"
                code = "1"
              }
            )
            .apply { text = "1" }
        mode = ListResource.ListMode.SNAPSHOT
        status = ListResource.ListStatus.CURRENT
        subject = patient0.asReference()
        date = Date()
        addEntry().apply {
          item = task0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-task"
                }
              )
              .apply { text = task0.referenceValue() }
        }
        addEntry().apply {
          item = enc0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-enc"
                }
              )
              .apply { text = enc0.referenceValue() }
        }
      }

    coEvery { fhirEngine.get(ResourceType.List, list0.logicalId) } returns list0
    coEvery { fhirEngine.get(ResourceType.Encounter, enc0.logicalId) } returns enc0
    coEvery { fhirEngine.get(ResourceType.Task, task0.logicalId) } returns task0

    val tracingOutcome = tracingRepository.getTracingOutcomes(0, list0.logicalId).single()
    Assert.assertEquals(
      TracingOutcome(
        historyId = list0.logicalId,
        encounterId = enc0.logicalId,
        title = "Home Tracing Outcome 1",
        date = enc0.period.start
      ),
      tracingOutcome
    )
  }

  @Test
  fun getHistoryDetailsReturnsExpected() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "little-john", 1, patientType = "exposed-infant")
    val task0 =
      Task().apply {
        id = "task0"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "miss-routine"
              display = "Missed Routine"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val enc0 =
      Encounter().apply {
        id = "enc0"
        status = Encounter.EncounterStatus.FINISHED
        class_ =
          Coding().apply {
            system = "https://d-tree.org"
            code = "IMP"
            display = "inpatient encounter"
          }
        serviceType =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = "home-tracing-outcome"
            }
          )
        subject = patient0.asReference()
        period = Period().apply { start = Date() }
        addReasonCode().apply {
          addCoding().apply {
            system = "https://d-tree.org"
            code = "miss-routine"
            display = "Missed Routine"
          }
          text = "Missed Routine"
        }
      }
    val obs0 =
      Observation().apply {
        id = "obs0"
        encounter = enc0.asReference()
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "https://d-tree.org"
              code = "tracing-outcome-conducted"
            }
          }
        value = CodeableConcept().apply { text = "conducted" }
      }
    val dateOfAppointmentObserved = DateTimeType.now()
    val obs1 =
      Observation().apply {
        id = "obs1"
        encounter = enc0.asReference()
        code =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = "home-tracing-outcome-date-of-agreed-appointment"
            }
          )
        value = dateOfAppointmentObserved
      }
    val list0 =
      ListResource().apply {
        id = "list0"
        title = "Tracing Encounter List_1"
        orderedBy =
          CodeableConcept(
              Coding().apply {
                system = "https://d-tree.org"
                code = "1"
              }
            )
            .apply { text = "1" }
        mode = ListResource.ListMode.SNAPSHOT
        status = ListResource.ListStatus.CURRENT
        subject = patient0.asReference()
        date = Date()
        addEntry().apply {
          item = task0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-task"
                }
              )
              .apply { text = task0.referenceValue() }
        }
        addEntry().apply {
          item = enc0.asReference()
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-enc"
                }
              )
              .apply { text = enc0.referenceValue() }
        }
      }

    coEvery { fhirEngine.get(ResourceType.List, list0.logicalId) } returns list0
    coEvery { fhirEngine.get(ResourceType.Encounter, enc0.logicalId) } returns enc0
    coEvery { fhirEngine.get(ResourceType.Task, task0.logicalId) } returns task0
    coEvery { fhirEngine.search<Observation>(Search(ResourceType.Observation)) } returns
      listOf(
        SearchResult(obs0, included = null, revIncluded = null),
        SearchResult(obs1, included = null, revIncluded = null)
      )

    val tracingOutcomeDetails = tracingRepository.getHistoryDetails(list0.logicalId, enc0.logicalId)
    Assert.assertTrue("Missed Routine" in tracingOutcomeDetails.reasons)
    Assert.assertTrue(tracingOutcomeDetails.conducted)
    Assert.assertEquals(enc0.period.start, tracingOutcomeDetails.date)
    Assert.assertEquals("conducted", tracingOutcomeDetails.outcome)
    Assert.assertEquals(dateOfAppointmentObserved.value, tracingOutcomeDetails.dateOfAppointment)
  }
}
