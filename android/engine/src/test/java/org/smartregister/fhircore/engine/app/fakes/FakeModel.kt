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

package org.smartregister.fhircore.engine.app.fakes

import java.util.Base64
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.util.getRandomBytesOfSize
import org.smartregister.fhircore.engine.util.toPasswordHash

object FakeModel {
  var salt = 256.getRandomBytesOfSize()
  val authCredentials =
    AuthCredentials(
      username = "demo",
      passwordHash = "51r1K4l1".toCharArray().toPasswordHash(salt),
      salt = Base64.getEncoder().encodeToString(salt),
      sessionToken = "49fad390491a5b547d0f782309b6a5b33f7ac087",
      refreshToken = "USrAgmSf5MJ8N_RLQODa7rZ3zNs1Sj1GkSIsTsb4n-Y",
    )
}
