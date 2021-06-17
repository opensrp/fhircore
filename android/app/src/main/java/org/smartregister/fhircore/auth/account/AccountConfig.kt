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
