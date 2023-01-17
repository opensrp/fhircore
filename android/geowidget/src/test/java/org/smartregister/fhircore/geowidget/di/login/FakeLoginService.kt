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

package org.smartregister.fhircore.geowidget.di.login

import androidx.appcompat.app.AppCompatActivity
import io.mockk.mockk
import org.smartregister.fhircore.engine.ui.login.LoginActivity
import org.smartregister.fhircore.engine.ui.login.LoginService

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 22-08-2022. */
class FakeLoginService
constructor(override var loginActivity: AppCompatActivity = mockk<LoginActivity>()) : LoginService {
  override fun navigateToHome() {}
  override fun fetchNonWorkflowConfigResources() {}
}
