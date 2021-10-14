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

package org.smartregister.fhirecore.quest

import androidx.test.core.app.ApplicationProvider
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestApplicationTest : RobolectricTest() {

  @Test
  fun testConstructFhirEngineShouldReturnNonNull() {
    Assert.assertNotNull(QuestApplication.getContext().fhirEngine)
  }
  @Test
  fun testThatApplicationIsInstanceOfConfigurableApplication() {
    Assert.assertTrue(
      ApplicationProvider.getApplicationContext<QuestApplication>() is ConfigurableApplication
    )
  }

  @Test
  fun testApplyConfigurationShouldLoadConfiguration() {
    val application = spyk(ApplicationProvider.getApplicationContext<QuestApplication>())

    val config = slot<ApplicationConfiguration>()
    application.applyApplicationConfiguration()

    verify { application.configureApplication(capture(config)) }

    Assert.assertEquals(QuestApplication.CONFIG_APP, config.captured.id)
  }

  @Test
  fun testResourceSyncParam() {
    val application = spyk(ApplicationProvider.getApplicationContext<QuestApplication>())

    val syncParam = application.resourceSyncParams
    Assert.assertEquals(5, syncParam.size)
    Assert.assertTrue(syncParam.containsKey(ResourceType.Patient))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Binary))
    Assert.assertTrue(syncParam.containsKey(ResourceType.CarePlan))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(syncParam.containsKey(ResourceType.QuestionnaireResponse))
  }
}
