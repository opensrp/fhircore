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

package org.smartregister.fhircore.quest.ui.report.measure.worker

import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.RepeatIntervalConfig
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule

@HiltAndroidTest
class MeasureReportWorkerTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()

  @Inject lateinit var workManager: WorkManager

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  lateinit var appConfig: ApplicationConfiguration
  var fhirEngine: FhirEngine = mockk()

  var fhirOperator: FhirOperator = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    appConfig =
      ApplicationConfiguration(
        appId = "ancApp",
        configType = "classification",
        theme = "dark theme",
        languages = listOf("en"),
        syncInterval = 15,
        appTitle = "Test App",
        remoteSyncPageSize = 100,
        reportRepeatInterval = RepeatIntervalConfig(10, 25)
      )
  }

  @Test fun checkIfWorkerStartedAtMentionedTime() {}
}
