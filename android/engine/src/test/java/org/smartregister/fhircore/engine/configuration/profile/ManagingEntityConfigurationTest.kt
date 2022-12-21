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

package org.smartregister.fhircore.engine.configuration.profile

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ExtractedResource

class ManagingEntityConfigurationTest {

  private val extractedResource =
    ExtractedResource(
      resourceType = "Patient",
      fhirPathExpression = "extractedResourceFhirPathExpression"
    )

  @Test
  fun testAuthConfiguration() {
    val managingEntityConfig =
      ManagingEntityConfig(
        infoFhirPathExpression = "sample fhir path expression",
        fhirPathResource = extractedResource,
        dialogTitle = "dialogTitle test",
        dialogWarningMessage = "dialogWarningMessage test",
        dialogContentMessage = "dialogContentMessage test"
      )

    Assert.assertEquals("sample fhir path expression", managingEntityConfig.infoFhirPathExpression)
    Assert.assertEquals(extractedResource, managingEntityConfig.fhirPathResource)
    Assert.assertEquals("dialogTitle test", managingEntityConfig.dialogTitle)
    Assert.assertEquals("dialogWarningMessage test", managingEntityConfig.dialogWarningMessage)
    Assert.assertEquals("dialogContentMessage test", managingEntityConfig.dialogContentMessage)
  }
}
