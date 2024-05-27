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

package org.smartregister.fhircore.engine.domain.model

import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.model.location.ChildTreeNode

data class LocationHierarchy(
  val identifier: String,
  val name: String,
  val children: List<LocationHierarchy> = listOf(),
) {
  override fun toString(): String {
    return name
  }

  companion object {
    fun fromLocationHierarchy(
      hierarchy: org.smartregister.model.location.LocationHierarchy,
    ): LocationHierarchy {
      val parentNode = hierarchy.locationHierarchyTree.locationsHierarchy.listOfNodes.treeNode
      return LocationHierarchy(
        identifier = parentNode.node?.referenceValue() ?: "",
        name = parentNode.node.name,
        children = parentNode.children.map { createLocation(it) },
      )
    }

    private fun createLocation(node: ChildTreeNode): LocationHierarchy {
      val parentNode = node.children
      val locations = mutableListOf<LocationHierarchy>()
      for (childNode in parentNode.children) {
        locations.add(createLocation(childNode))
      }
      return LocationHierarchy(
        identifier = parentNode.node?.referenceValue() ?: "",
        name = parentNode.node.name,
        children = locations,
      )
    }
  }
}
