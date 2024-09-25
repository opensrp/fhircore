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

import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestXFhirQueryResolverTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testQuestXFhirQueryResolver() =
    runTest(timeout = 120.seconds) {
      val patient = Patient().apply { setActive(true) }
      val task = Task()
      fhirEngine.create(patient, task)
      val xFhirResolver = QuestXFhirQueryResolver(fhirEngine)
      val result: List<Resource> = xFhirResolver.resolve("Patient?active=true")
      Assert.assertTrue(result.isNotEmpty())
      Assert.assertTrue(
        result.all { it.resourceType == ResourceType.Patient },
      )
    }
}
