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
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.domain.model.ViewType

class ViewPropertiesKtTest {

  @Test
  fun tesRetrieveListPropertiesShouldReturnNestedLists() {
    val listProperties =
      listOf(
        ListProperties(
          id = "list0",
          viewType = ViewType.LIST,
          registerCard =
            RegisterCardConfig(
              views =
                listOf(
                  ListProperties(
                    id = "list2",
                    viewType = ViewType.LIST,
                    registerCard = RegisterCardConfig()
                  ),
                  ColumnProperties(
                    viewType = ViewType.COLUMN,
                    children =
                      listOf(
                        ListProperties(
                          id = "list3",
                          viewType = ViewType.LIST,
                          registerCard = RegisterCardConfig()
                        )
                      )
                  )
                )
            )
        ),
        ListProperties(id = "list1", viewType = ViewType.LIST, registerCard = RegisterCardConfig())
      )

    val retrievedListProperties = listProperties.retrieveListProperties()
    Assert.assertEquals(4, retrievedListProperties.size)
    retrievedListProperties.forEachIndexed { index, it -> Assert.assertEquals("list$index", it.id) }
  }
}
