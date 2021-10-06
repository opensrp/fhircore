/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.ui.login

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.setAppLocale

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
      navigateToHome.observe(this@BaseLoginActivity, { navigateToHome() })
    }

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
  }

  override fun attachBaseContext(baseContext: Context) {
    val lang =
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    baseContext.setAppLocale(lang).run {
      super.attachBaseContext(baseContext)
      applyOverrideConfiguration(this)
    }
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  abstract fun navigateToHome()

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }
}
