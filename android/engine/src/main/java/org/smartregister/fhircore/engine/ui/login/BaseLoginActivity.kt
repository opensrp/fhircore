package org.smartregister.fhircore.engine.ui.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.runSync

abstract class BaseLoginActivity :
  ComponentActivity(), ConfigurableComposableView<LoginViewConfiguration> {

  private lateinit var loginViewModel: LoginViewModel

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    application.assertIsConfigurable()

    loginViewModel =
      ViewModelProvider(
        this,
        LoginViewModel(
            application = application,
            authenticationService = (application as ConfigurableApplication).authenticationService,
            loginViewConfiguration = loginViewConfigurationOf()
          )
          .createFactory()
      )[LoginViewModel::class.java]

    loginViewModel.apply {
      loginUser()
      navigateToHome.observe(
        this@BaseLoginActivity,
        {
          lifecycleScope.launch(dispatcherProvider.io()) { application.runSync() } // initiate sync
          navigateToHome()
        }
      )
    }

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  abstract fun navigateToHome()

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }
}
