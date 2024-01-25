package org.smartregister.fhircore.quest.cucumber.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.smartregister.fhircore.quest.cucumber.objects.Login

class LoginSteps {
    val login = Login()
    val applicationID = "quest"

    @Given("I am on the application settings page")
    fun i_am_on_the_application_settings_page() {
        login.composeTestAppScreen
    }
    @When("I enter an application id")
    fun i_enter_an_application_id() {
        login.enterApplicationIdField(applicationID)
    }
    @When("I tap the \"Load application settings\" button")
    fun i_tap_the_load_settings_button() {
        login.clickLoadConfigurationsButton()
    }
    @Given("I am on the login page")
    fun i_am_on_the_login_page() {
        // TODO: Implement this step
    }

    @When("I enter my username and password")
    fun i_enter_my_username_and_password() {
        // TODO: Implement this step
    }

    @When("I tap the \"Login\" button")
    fun i_tap_the_login_button() {
        // TODO: Implement this step
    }

    @Then("I should be logged in")
    fun i_should_be_logged_in() {
        // TODO: Implement this step
    }

    @Then("I should see an error message")
    fun i_should_see_an_error_message() {
        // TODO: Implement this step
    }
}