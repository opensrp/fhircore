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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TriStateCheckbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import java.util.LinkedList

@Composable
fun <T> ColumnScope.MultiSelectView(
  rootTreeNode: TreeNode<T>,
  selectedNodes: MutableMap<String, ToggleableState>,
  depth: Int = 0,
  content: @Composable (TreeNode<T>) -> Unit,
) {
  val collapsedState = remember { mutableStateOf(false) }
  MultiSelectCheckbox(
    selectedNodes = selectedNodes,
    currentTreeNode = rootTreeNode,
    depth = depth,
    content = content,
    collapsedState = collapsedState,
  )
  if (collapsedState.value) {
    rootTreeNode.children.forEach {
      MultiSelectView(
        rootTreeNode = it,
        selectedNodes = selectedNodes,
        depth = depth + 16,
        content = content,
      )
    }
  }
}

@Composable
fun <T> MultiSelectCheckbox(
  selectedNodes: MutableMap<String, ToggleableState>,
  currentTreeNode: TreeNode<T>,
  depth: Int,
  content: @Composable (TreeNode<T>) -> Unit,
  collapsedState: MutableState<Boolean>,
) {
  val checked = remember { mutableStateOf(false) }
  Column {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = depth.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (currentTreeNode.children.isNotEmpty()) {
        Icon(
          imageVector =
            if (collapsedState.value) {
              Icons.Default.ArrowDropDown
            } else Icons.AutoMirrored.Filled.ArrowRight,
          contentDescription = null,
          tint = Color.Gray,
          modifier = Modifier.clickable { collapsedState.value = !collapsedState.value },
        )
      }

      TriStateCheckbox(
        state = selectedNodes[currentTreeNode.id] ?: ToggleableState.Off,
        onClick = {
          selectedNodes[currentTreeNode.id] = ToggleableState(!checked.value)
          checked.value = selectedNodes[currentTreeNode.id] == ToggleableState.On

          var toggleableState: ToggleableState
          var parent = currentTreeNode.parent
          while (parent != null) {
            toggleableState = ToggleableState.Indeterminate
            if (
              parent.children.all {
                selectedNodes[it.id] == ToggleableState.Off || selectedNodes[it.id] == null
              }
            ) {
              toggleableState = ToggleableState.Off
            }
            if (parent.children.all { selectedNodes[it.id] == ToggleableState.On }) {
              toggleableState = ToggleableState.On
            }
            selectedNodes[parent.id] = toggleableState
            parent = parent.parent
          }

          // Select all the nested checkboxes
          val linkedList = LinkedList(currentTreeNode.children)

          while (linkedList.isNotEmpty()) {
            val currentNode = linkedList.removeFirst()
            selectedNodes[currentNode.id] = ToggleableState(checked.value)
            currentNode.children.forEach { linkedList.add(it) }
          }
        },
        modifier = Modifier.padding(0.dp),
        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary),
        interactionSource = remember { MutableInteractionSource() },
      )
      content(currentTreeNode)
    }
  }
}
