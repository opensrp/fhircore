package org.smartregister.fhircore.engine.app

import javax.inject.Inject
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.login.LoginService

class AppLoginService @Inject constructor() : LoginService {

  override lateinit var loginActivity: LoginActivity

  override fun navigateToHome() {
    // Do nothing
  }
}
