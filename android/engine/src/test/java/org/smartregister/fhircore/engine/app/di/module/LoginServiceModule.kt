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

package org.smartregister.fhircore.engine.app.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import io.mockk.spyk
import org.smartregister.fhircore.quest.ui.login.LoginActivityTest
import org.smartregister.fhircore.quest.ui.login.LoginService

@InstallIn(ActivityComponent::class)
@Module
object LoginServiceModule {

  @Provides
  fun bindLoginService(): org.smartregister.fhircore.quest.ui.login.LoginService =
    spyk(org.smartregister.fhircore.quest.ui.login.LoginActivityTest.TestLoginService())
}
