/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.data

import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.migration.MigrationConfig
import org.smartregister.fhircore.engine.configuration.migration.UpdateValueConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.plusDays
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class DataMigrationTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var preferenceDataStore: PreferenceDataStore

  @Inject lateinit var dataMigration: DataMigration

  private val patient =
    Faker.buildPatient().apply { gender = Enumerations.AdministrativeGender.MALE }

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testMigrateShouldUpdateResources() =
    runTest(timeout = 45.seconds) {
      // Create patient to be updated
      defaultRepository.create(addResourceTags = true, patient)
      dataMigration.migrate(
        migrationConfigs =
          listOf(
            MigrationConfig(
              resourceConfig =
                FhirResourceConfig(
                  baseResource = ResourceConfig(resource = ResourceType.Patient),
                ),
              version = 7,
              rules =
                listOf(
                  RuleConfig(name = "value", actions = listOf("data.put('value', 'female')")),
                ),
              updateValues =
                listOf(
                  UpdateValueConfig(
                    jsonPathExpression = "\$.gender",
                    computedValueKey = "value",
                  ),
                ),
            ),
            MigrationConfig(
              resourceConfig =
                FhirResourceConfig(
                  baseResource = ResourceConfig(resource = ResourceType.Patient),
                ),
              version = 1,
              rules =
                listOf(
                  RuleConfig(name = "value", actions = listOf("data.put('value', 'female')")),
                ),
              updateValues =
                listOf(
                  UpdateValueConfig(
                    jsonPathExpression = "\$.gender",
                    computedValueKey = "value",
                  ),
                ),
            ),
          ),
        previousVersion = 0,
      )
      // Patient gender should be updated from 'male' to 'female'
      val updatedPatient = defaultRepository.loadResource<Patient>(patient.logicalId)
      Assert.assertTrue(updatedPatient?.gender != patient.gender)
      Assert.assertEquals(Enumerations.AdministrativeGender.FEMALE, updatedPatient?.gender)

      // Version updated to 7 (the maximum migration version)
      Assert.assertEquals(
        7,
        preferenceDataStore.read(PreferenceDataStore.MIGRATION_VERSION).first(),
      )
    }

  @Test
  fun testTaskBasedOnReferenceIsUpdated() =
    runTest(timeout = 60.seconds) {
      val carePlan: CarePlan =
        CarePlan().apply {
          id = "careplan-1"
          identifier =
            mutableListOf(
              Identifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                value = "value-1"
              },
            )
          subject = patient.asReference()
        }

      val taskId = UUID.randomUUID().toString()
      val task =
        Task().apply {
          id = taskId
          status = Task.TaskStatus.READY
          executionPeriod =
            Period().apply {
              start = Date().plusMonths(-1)
              end = Date().plusDays(-1)
            }
          addBasedOn(Reference(carePlan.logicalId)) // Wrong Reference missing ResourceType CarePlan
        }

      defaultRepository.create(addResourceTags = true, patient, carePlan, task)

      // Fix Task.basedOn reference FROM "careplan-1" TO "CarePlan/careplan-1"
      dataMigration.migrate(
        migrationConfigs =
          listOf(
            MigrationConfig(
              version = 1,
              resourceConfig =
                FhirResourceConfig(
                  baseResource = ResourceConfig(resource = ResourceType.Task),
                ),
              updateValues =
                listOf(
                  UpdateValueConfig(
                    // Expression should be exact; '$.basedOn[0].reference' is also valid
                    // JsonPath allows replacing JSON elements on provided path. FHIRPath doesn't.
                    jsonPathExpression = "Task.basedOn[0].reference",
                    computedValueKey = "value",
                  ),
                ),
              rules =
                listOf(
                  RuleConfig(
                    name = "value",
                    actions =
                      listOf(
                        "data.put('value', 'CarePlan/' + fhirPath.extractValue(Task, 'Task.basedOn.first().reference'))",
                      ),
                  ),
                ),
            ),
          ),
        previousVersion = 0,
      )

      val updatedTask = defaultRepository.loadResource<Task>(taskId)
      Assert.assertNotNull(updatedTask?.basedOn)
      Assert.assertEquals("CarePlan/${carePlan.logicalId}", updatedTask?.basedOnFirstRep?.reference)

      // Version updated to 1
      Assert.assertEquals(
        1,
        preferenceDataStore.read(PreferenceDataStore.MIGRATION_VERSION).first(),
      )
    }
}
