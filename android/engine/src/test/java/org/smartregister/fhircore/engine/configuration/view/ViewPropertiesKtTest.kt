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
