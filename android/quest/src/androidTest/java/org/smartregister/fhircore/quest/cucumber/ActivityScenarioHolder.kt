package org.smartregister.fhircore.quest.cucumber

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.cucumber.java.After
import io.cucumber.junit.WithJunitRule
import org.junit.Before
import org.junit.Rule
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingActivity
import org.smartregister.fhircore.quest.ui.appsetting.AppSettingScreen

@WithJunitRule
@HiltAndroidTest
class ActivityScenarioHolder {

  //@get:Rule(order = 1)
  //val hiltRule = HiltAndroidRule(this)


  private var scenario :ActivityScenario<*>? = null
  fun launch(intent: Intent) {
    scenario = ActivityScenario.launch<Activity>(intent)
  }

  /*
  @io.cucumber.java.Before
  fun setup() {
    hiltRule.inject()
  }
  */


  @After
  fun close() {
    scenario?.close()
  }
}