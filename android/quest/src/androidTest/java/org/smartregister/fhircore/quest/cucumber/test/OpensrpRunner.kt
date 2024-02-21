package org.smartregister.fhircore.quest.cucumber.test

import android.app.Application
import android.content.Context
import android.os.Bundle
import dagger.hilt.android.testing.HiltTestApplication
import io.cucumber.android.runner.CucumberAndroidJUnitRunner
import io.cucumber.junit.CucumberOptions
import org.smartregister.fhircore.quest.QuestApplication

//, glue = ["org.smartregister.fhircore.quest.cucumber.steps"

@CucumberOptions(features = ["features"], glue = ["org.smartregister.fhircore.quest.cucumber.steps"])
class OpensrpRunner: CucumberAndroidJUnitRunner() {


  override fun onCreate(bundle: Bundle?) {
    super.onCreate(bundle)
  }

  override fun newApplication(
    cl: ClassLoader?,
    className: String?,
    context: Context?
  ): Application {
    return super.newApplication(cl, QuestApplication::class.simpleName, context)
  }
}