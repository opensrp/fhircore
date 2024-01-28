package org.smartregister.fhircore.quest.cucumber.test

import android.app.Application
import android.content.Context
import android.os.Bundle
import io.cucumber.android.runner.CucumberAndroidJUnitRunner
import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberJUnitRunner
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith
import org.smartregister.fhircore.quest.QuestApplication

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["features"],
    glue = ["org.smartregister.fhircore.quest"],
    plugin = ["pretty", "json:build/reports/cucumber.json"]
)
class Runner : CucumberAndroidJUnitRunner() {
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