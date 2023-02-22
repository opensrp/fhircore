/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration.view

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ViewType

class PersonalDataPropertiesTest {

  private val personalDataItemList = emptyList<PersonalDataItem>()
  private val personalDataProperties =
    PersonalDataProperties(
      viewType = ViewType.PERSONAL_DATA,
      weight = 0f,
      backgroundColor = "#FFFFFF",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = false,
      fillMaxHeight = false,
      clickable = "false",
      personalDataItems = personalDataItemList
    )

  @Test
  fun testPersonalDataProperties() {
    Assert.assertEquals(ViewType.PERSONAL_DATA, personalDataProperties.viewType)
    Assert.assertEquals(0f, personalDataProperties.weight)
    Assert.assertEquals("#FFFFFF", personalDataProperties.backgroundColor)
    Assert.assertEquals(0, personalDataProperties.padding)
    Assert.assertEquals(2, personalDataProperties.borderRadius)
    Assert.assertEquals(ViewAlignment.NONE, personalDataProperties.alignment)
    Assert.assertEquals(false, personalDataProperties.fillMaxWidth)
    Assert.assertEquals(false, personalDataProperties.fillMaxHeight)
    Assert.assertEquals("false", personalDataProperties.clickable)
    Assert.assertEquals(personalDataItemList, personalDataProperties.personalDataItems)
  }
}
