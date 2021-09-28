package org.smartregister.quest.ui.login

import android.os.Bundle
import org.smartregister.fhircore.engine.configuration.view.loginViewConfigurationOf
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.quest.R

class LoginActivity : BaseLoginActivity() {
  override fun navigateToHome() {
    this.showToast("Show quest")
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      loginViewConfigurationOf(
        applicationName = getString(R.string.app_name),
        applicationVersion = BuildConfig.VERSION_NAME,
        darkMode = false
      )
    )
  }
}
