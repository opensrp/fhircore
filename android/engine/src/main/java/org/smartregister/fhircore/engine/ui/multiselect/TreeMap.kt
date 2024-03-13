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

package org.smartregister.fhircore.engine.ui.multiselect

import androidx.compose.runtime.Stable

@Stable
data class TreeNode<T>(
  val id: String,
  var parentId: String?,
  val data: T,
  val children: MutableList<TreeNode<T>> = mutableListOf(),
)

object TreeMap {

  fun <T> populateLookupMap(
    items: List<TreeNode<T>>,
    lookup: MutableMap<String, TreeNode<T>>,
  ): Map<String, TreeNode<T>> {
    items.forEach { item ->
      val childNode = findOrCreate(item.id, item, lookup)
      val parentNode = findOrCreate(item.parentId, item, lookup)
      if (parentNode != null && childNode != null) {
        parentNode.children.add(childNode)
        childNode.parentId = parentNode.id
      }
    }
    return lookup
  }

  private fun <T> findOrCreate(
    id: String?,
    lookupItem: TreeNode<T>,
    lookup: MutableMap<String, TreeNode<T>>,
  ): TreeNode<T>? {
    if (id.isNullOrEmpty()) return null
    return lookup.getOrPut(id) {
      TreeNode(
        id = id,
        parentId = null,
        data = lookupItem.data,
        children = mutableListOf(),
      )
    }
  }
}
