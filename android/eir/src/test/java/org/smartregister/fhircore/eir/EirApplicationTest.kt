package org.smartregister.fhircore.eir

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication

@Config(shadows = [EirApplicationShadow::class])
class EirApplicationTest : RobolectricTest() {

  @Test
  fun testConstructFhirEngineShouldReturnNonNull() {
    WorkManager.initialize(EirApplication.getContext(), Configuration.Builder().build())
    Assert.assertNotNull(EirApplication.getContext().fhirEngine)
  }
  @Test
  fun testThatApplicationIsInstanceOfConfigurableApplication() {
    Assert.assertTrue(
      ApplicationProvider.getApplicationContext<EirApplication>() is ConfigurableApplication
    )
  }
}
