package org.smartregister.fhircore.engine.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.BaseUnitTest

class AuthAndroidServiceTest : BaseUnitTest() {

  private val authAndroidService: AuthAndroidService = spyk(AuthAndroidService())

  private val accountAuthenticator: AccountAuthenticator = mockk(relaxed = true)

  @Test
  fun testOnBindFunctionShouldCallAuthenticatorBinder() {
    every { authAndroidService.accountAuthenticator } returns accountAuthenticator
    Assert.assertEquals(authAndroidService.onBind(null), accountAuthenticator.iBinder)
  }
}
