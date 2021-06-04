package org.smartregister.fhircore.auth.account

import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.LoginActivity

object AccountConfig {
  const val KEY_IS_NEW_ACCOUNT: String = "IS_NEW_ACCOUNT"
  const val KEY_AUTH_TOKEN_TYPE: String = "AUTH_TOKEN_TYPE"
  const val AUTH_TOKEN_TYPE: String = "ACCESS_TOKEN"

  val AUTH_HANDLER_ACTIVITY = LoginActivity::class.java
  val ACCOUNT_TYPE: String = FhirApplication.getContext().getString(R.string.authenticator_account_type)

}
