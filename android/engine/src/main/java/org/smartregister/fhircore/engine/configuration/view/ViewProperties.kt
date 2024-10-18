/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

/**
 * An abstract for view properties. This is needed so we can serialize/deserialize view properties
 * map into different data classes. Common view properties MUST be implemented by subclasses for
 * access.
 */
@Serializable(with = ViewPropertiesSerializer::class)
abstract class ViewProperties : java.io.Serializable {
  abstract val viewType: ViewType
  abstract val weight: Float
  abstract val backgroundColor: String?
  abstract val padding: Int
  abstract val borderRadius: Int
  abstract val alignment: ViewAlignment
  abstract val fillMaxWidth: Boolean
  abstract val fillMaxHeight: Boolean
  abstract val clickable: String
  abstract val visible: String
  abstract val opacity: Float?

  abstract fun interpolate(computedValuesMap: Map<String, Any>): ViewProperties
}

/**
 * This function obtains all [ListProperties] from the [ViewProperties] list; including the nested
 * LISTs
 */
fun List<ViewProperties>.retrieveListProperties(): List<ListProperties> {
  val listProperties = mutableListOf<ListProperties>()
  val viewPropertiesQueue: ArrayDeque<ViewProperties> = ArrayDeque(this)
  while (viewPropertiesQueue.isNotEmpty()) {
    val properties = viewPropertiesQueue.removeFirst()
    if (properties.viewType == ViewType.LIST) {
      listProperties.add(properties as ListProperties)
    }
    when (properties.viewType) {
      ViewType.COLUMN -> viewPropertiesQueue.addAll((properties as ColumnProperties).children)
      ViewType.ROW -> viewPropertiesQueue.addAll((properties as RowProperties).children)
      ViewType.CARD -> viewPropertiesQueue.addAll((properties as CardViewProperties).content)
      ViewType.LIST -> viewPropertiesQueue.addAll((properties as ListProperties).registerCard.views)
      else -> {}
    }
  }
  return listProperties
}

enum class ViewAlignment {
  START,
  END,
  CENTER,
  NONE,
  TOPSTART,
  TOPEND,
  BOTTOMSTART,
  BOTTOMEND,
}
