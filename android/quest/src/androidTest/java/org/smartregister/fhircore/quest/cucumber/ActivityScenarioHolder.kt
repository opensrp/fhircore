package org.smartregister.fhircore.quest.cucumber

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import io.cucumber.junit.WithJunitRule

@WithJunitRule
class ActivityScenarioHolder {
  private var scenario :ActivityScenario<*>? = null
  fun launch(intent: Intent) {
    scenario = ActivityScenario.launch<Activity>(intent)
  }
  @org.junit.After
  fun close() {
    scenario?.close()
  }
}