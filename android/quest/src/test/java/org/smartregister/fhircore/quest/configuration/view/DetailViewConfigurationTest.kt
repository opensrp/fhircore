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

package org.smartregister.fhircore.quest.configuration.view

import org.hl7.fhir.r4.model.Enumerations
import org.junit.Assert
import org.junit.Test

class DetailViewConfigurationTest {

  @Test
  fun testDetailViewConfiguration() {

    val filters =
      listOf(
        Filter(
          resourceType = Enumerations.ResourceType.RESOURCE,
          key = "key-1",
          displayableProperty = "label",
          valuePrefix = "re",
          valuePostfix = "end",
          valueType = Enumerations.DataType.ADDRESS
        )
      )

    val detailViewConfiguration =
      DetailViewConfiguration(
        appId = "quest",
        classification = "registration",
        label = "patient registration",
        rows = listOf(DetailViewRowConfiguration(filters))
      )

    Assert.assertEquals("quest", detailViewConfiguration.appId)
    Assert.assertEquals("registration", detailViewConfiguration.classification)
    Assert.assertEquals("patient registration", detailViewConfiguration.label)

    val actualFilter = detailViewConfiguration.rows.get(0).filters.get(0)

    Assert.assertEquals(Enumerations.ResourceType.RESOURCE, actualFilter.resourceType)
    Assert.assertEquals("key-1", actualFilter.key)
    Assert.assertEquals("label", actualFilter.displayableProperty)
    Assert.assertEquals("re", actualFilter.valuePrefix)
    Assert.assertEquals("end", actualFilter.valuePostfix)
    Assert.assertEquals(Enumerations.DataType.ADDRESS, actualFilter.valueType)
  }
}
