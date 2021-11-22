package org.smartregister.fhircore.engine.app.fakes

import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.util.toSha1

object FakeModel {
  val authCredentials =
    AuthCredentials(
      username = "demo",
      password = "51r1K4l1".toSha1(),
      sessionToken = "49fad390491a5b547d0f782309b6a5b33f7ac087",
      refreshToken = "USrAgmSf5MJ8N_RLQODa7rZ3zNs1Sj1GkSIsTsb4n-Y"
    )
}
