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

package org.smartregister.fhircore.eir.shadow

import androidx.security.crypto.MasterKey
import io.mockk.mockk
import org.robolectric.Shadows
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@Implements(SecureSharedPreference::class)
class SecureSharedPreferenceShadow : Shadows() {

  @Implementation
  fun retrieveSessionUsername(): String {
    return "demo"
  }

  @Implementation
  fun getMasterKey(): MasterKey {
    return mockk<MasterKey>()
  }
}
