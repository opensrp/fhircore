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

package org.smartregister.fhircore.eir

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.smartregister.fhircore.engine.auth.AccountAuthenticator

class EirAuthAndroidService : Service() {

  private lateinit var authenticator: AccountAuthenticator

  override fun onCreate() {
    authenticator = AccountAuthenticator(this, EirAuthenticationService(this))
  }

  override fun onBind(intent: Intent?): IBinder {
    return authenticator.iBinder
  }
}
