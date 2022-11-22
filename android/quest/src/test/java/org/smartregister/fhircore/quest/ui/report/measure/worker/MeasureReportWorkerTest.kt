package org.smartregister.fhircore.quest.ui.report.measure.worker

import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
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
            reportRepeatInterval = RepeatIntervalConfig(10, 25))

  }

  @Test
  fun checkIfWorkerStartedAtMentionedTime(){
  }

}
