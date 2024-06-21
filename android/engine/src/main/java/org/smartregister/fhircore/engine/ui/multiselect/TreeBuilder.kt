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
class TreeNode<T>(
  val id: String,
  var parent: TreeNode<T>?,
  val data: T,
  val children: MutableList<TreeNode<T>> = mutableListOf(),
)

object TreeBuilder {

  /** This function creates and return a list of root [TreeNode]'s */
  fun <T> buildTrees(
    items: List<TreeNode<T>>,
    rootNodeIds: Set<String>,
  ): List<TreeNode<T>> {
    val lookupMap = mutableMapOf<String, TreeNode<T>>()
    items.forEach { item ->
      val childNode = findOrCreate(item, lookupMap)
      val parentNode = findOrCreate(item.parent, lookupMap)
      if (parentNode != null && childNode != null) {
        parentNode.children.add(childNode)
        childNode.parent = parentNode
      }
    }
    return rootNodeIds.mapNotNull { lookupMap[it] }
  }

  private fun <T> findOrCreate(
    treeNode: TreeNode<T>?,
    lookupMap: MutableMap<String, TreeNode<T>>,
  ): TreeNode<T>? {
    treeNode?.let { node ->
      return lookupMap.getOrPut(node.id) {
        TreeNode(
          id = node.id,
          parent = null,
          data = node.data,
          children = mutableListOf(),
        )
      }
    }
    return null
  }
}
