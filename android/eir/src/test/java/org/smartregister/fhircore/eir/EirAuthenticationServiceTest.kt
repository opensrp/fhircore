package org.smartregister.fhircore.eir

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.ui.login.LoginActivity
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf

class EirAuthenticationServiceTest : RobolectricTest() {

  private lateinit var eirAuthenticationService: EirAuthenticationService

  @Before
  fun setUp() {
    ReflectionHelpers.setField(
      ApplicationProvider.getApplicationContext(),
      "applicationConfiguration",
      applicationConfigurationOf().apply {
        clientId = "clientId"
        clientSecret = "clientSecret"
        scope = "openid"
      }
    )
    eirAuthenticationService = EirAuthenticationService(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testThatConfigsAreNotNull() {
    Assert.assertNotNull(eirAuthenticationService.getApplicationConfigurations())

    Assert.assertNotNull(eirAuthenticationService.clientId())
    Assert.assertEquals("clientId", eirAuthenticationService.clientId())

    Assert.assertNotNull(eirAuthenticationService.clientSecret())
    Assert.assertEquals("clientSecret", eirAuthenticationService.clientSecret())

    Assert.assertNotNull(eirAuthenticationService.providerScope())
    Assert.assertEquals("openid", eirAuthenticationService.providerScope())

    Assert.assertEquals(
      eirAuthenticationService.getLoginActivityClass().simpleName,
      LoginActivity::class.simpleName
    )

    Assert.assertEquals("org.smartregister.fhircore.eir", eirAuthenticationService.getAccountType())
  }
}
