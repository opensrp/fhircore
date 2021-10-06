package org.smartregister.fhircore.engine.ui.login

import android.accounts.AccountManagerCallback
import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

internal class LoginViewModelTest : RobolectricTest() {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application: Application = ApplicationProvider.getApplicationContext()

  private lateinit var loginViewModel: LoginViewModel

  private lateinit var configurableApplication: ConfigurableApplication

  @Before
  fun setUp() {
    configurableApplication = application as ConfigurableApplication
    loginViewModel =
      spyk(
        LoginViewModel(
          application = ApplicationProvider.getApplicationContext(),
          authenticationService = configurableApplication.authenticationService,
          loginViewConfiguration = loginViewConfigurationOf()
        )
      )

    loginViewModel.updateViewConfigurations(loginViewConfigurationOf())
  }

  @Test
  fun testThatViewModelIsInitialized() {
    Assert.assertNotNull(loginViewModel)
  }

  @Test
  fun testOnPasswordChanged() {
    val newPassword = "NewP455W0rd"
    loginViewModel.onPasswordUpdated(newPassword)
    Assert.assertNotNull(loginViewModel.password.value)
    Assert.assertEquals(newPassword, loginViewModel.password.value)
  }

  @Test
  fun testOnUsernameChanged() {
    val username = "username"
    loginViewModel.onUsernameUpdated(username)
    Assert.assertNotNull(loginViewModel.username.value)
    Assert.assertEquals(username, loginViewModel.username.value)
  }

  @Test
  fun testApplicationConfiguration() {
    val coolAppName = "Cool App"
    loginViewModel.updateViewConfigurations(loginViewConfigurationOf(applicationName = coolAppName))
    Assert.assertNotNull(loginViewModel.loginViewConfiguration.value)
    Assert.assertEquals(coolAppName, loginViewModel.loginViewConfiguration.value?.applicationName)
  }
}
