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
  val parentId: String?,
  val data: T,
  val children: List<String>? = null,
)

@Stable
data class LookupItem<T>(
  val id: String,
  val parentId: String?,
  val data: T,
)

object TreeMap {

  fun <T> populateLookupMap(
    items: List<LookupItem<T>>,
    lookup: MutableMap<String, TreeNode<T>>,
  ): MutableMap<String, TreeNode<T>> {
    items.forEach {
      val childNode = findOrCreate(it.id, it, lookup)
      val parentNode = findOrCreate(it.parentId, it, lookup)
      if (childNode != null) {
        lookup[childNode.id] = childNode.copy(parentId = parentNode?.id)
      }
      if (parentNode != null && childNode != null) {
        lookup[parentNode.id] = parentNode.copy(children = parentNode.children?.plus(childNode.id))
      }
    }
    return lookup
  }

  private fun <T> findOrCreate(
    id: String?,
    lookupItem: LookupItem<T>,
    lookup: MutableMap<String, TreeNode<T>>,
  ): TreeNode<T>? {
    if (id.isNullOrEmpty()) return null
    return lookup.getOrPut(id) { TreeNode(id, null, lookupItem.data, children = mutableListOf()) }
  }
}
