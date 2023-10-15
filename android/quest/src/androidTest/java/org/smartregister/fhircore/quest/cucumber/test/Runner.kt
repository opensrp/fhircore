package org.smartregister.fhircore.quest.cucumber.test

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["features"],
    glue = ["steps"],
    plugin = ["pretty", "json:build/reports/cucumber.json"]
)
class Runner {
}