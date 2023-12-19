/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.migration.MigrationConfig
import org.smartregister.fhircore.engine.configuration.migration.UpdateValueConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class DataMigrationTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var dataMigration: DataMigration

  private val patient = Faker.buildPatient()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testMigrateShouldUpdateResources() = runTest {
    // Create patient to be updated
    defaultRepository.create(addResourceTags = true, patient)
    dataMigration.migrate(
      migrationConfig =
        MigrationConfig(
          resourceType = ResourceType.Patient,
          dataQueries = emptyList(),
          version = 1,
          updateValues =
            listOf(
              UpdateValueConfig(
                jsonPathExpression = "\$.gender",
                valueRule =
                  RuleConfig(name = "value", actions = listOf("data.put('value', 'female')")),
              ),
            ),
        ),
      latestMigrationVersion = 1,
    )
    // Patient gender should be updated
    val updatedPatient = defaultRepository.loadResource<Patient>(patient.logicalId)
    Assert.assertTrue(updatedPatient?.gender != patient.gender)
    Assert.assertEquals(Enumerations.AdministrativeGender.FEMALE, updatedPatient?.gender)
  }
}
