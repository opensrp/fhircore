package org.smartregister.fhircore.quest.tests

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.smartregister.fhircore.engine.ui.appsetting.AppSettingActivity

@RunWith(Suite::class)
@Suite.SuiteClasses(*[LaunchActivityTest::class,
    SettingTest::class])
class ESP_start {

}