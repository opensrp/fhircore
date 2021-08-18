package org.smartregister.fhircore.eir

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.eir.robolectric.RobolectricTest

class EirAuthAndroidServiceTest : RobolectricTest() {

  private lateinit var accountService: EirAuthAndroidService

  @Before
  fun setUp() {
    accountService =
      Robolectric.buildService(EirAuthAndroidService::class.java, null).create().get()
  }

  @Test
  fun testOnCreateShouldCreateAccountAuthenticatorObject() {
    val field = accountService.javaClass.getDeclaredField("authenticator")
    field.isAccessible = true
    Assert.assertNotNull(field.get(accountService))
  }

  @Test
  fun testOnBindShouldReturnNonNullIBinder() {
    Assert.assertNotNull(accountService.onBind(null))
  }
}
