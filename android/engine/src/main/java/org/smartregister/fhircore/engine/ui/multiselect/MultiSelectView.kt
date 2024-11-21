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
import org.smartregister.fhircore.engine.domain.model.SyncLocationState

@Composable
fun <T> ColumnScope.MultiSelectView(
  rootTreeNode: TreeNode<T>,
  syncLocationStateMap: MutableMap<String, SyncLocationState>,
  depth: Int = 0,
  onChecked: () -> Unit,
  content: @Composable (TreeNode<T>) -> Unit,
) {
  val collapsedState = remember { mutableStateOf(false) }
  MultiSelectCheckbox(
    syncLocationStateMap = syncLocationStateMap,
    currentTreeNode = rootTreeNode,
    depth = depth,
    content = content,
    collapsedState = collapsedState,
    onChecked = onChecked,
  )
  if (collapsedState.value) {
    rootTreeNode.children.forEach {
      MultiSelectView(
        rootTreeNode = it,
        syncLocationStateMap = syncLocationStateMap,
        depth = depth + 16,
        content = content,
        onChecked = onChecked,
      )
    }
  }
}

@Composable
private fun <T> MultiSelectCheckbox(
  syncLocationStateMap: MutableMap<String, SyncLocationState>,
  currentTreeNode: TreeNode<T>,
  depth: Int,
  content: @Composable (TreeNode<T>) -> Unit,
  collapsedState: MutableState<Boolean>,
  onChecked: () -> Unit,
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
            } else {
              Icons.AutoMirrored.Filled.ArrowRight
            },
          contentDescription = null,
          tint = Color.Gray,
          modifier = Modifier.clickable { collapsedState.value = !collapsedState.value },
        )
      }

      TriStateCheckbox(
        state = syncLocationStateMap[currentTreeNode.id]?.toggleableState ?: ToggleableState.Off,
        onClick = {
          syncLocationStateMap[currentTreeNode.id] =
            SyncLocationState(
              currentTreeNode.id,
              currentTreeNode.parent?.id,
              ToggleableState(
                syncLocationStateMap[currentTreeNode.id]?.toggleableState != ToggleableState.On,
              ),
            )
          checked.value =
            syncLocationStateMap[currentTreeNode.id]?.toggleableState == ToggleableState.On

          var toggleableState: ToggleableState
          var parent = currentTreeNode.parent
          while (parent != null) {
            toggleableState = ToggleableState.Indeterminate
            if (
              parent.children.all {
                syncLocationStateMap[it.id]?.toggleableState == ToggleableState.Off ||
                  syncLocationStateMap[it.id] == null
              }
            ) {
              toggleableState = ToggleableState.Off
            }
            if (
              parent.children.all {
                syncLocationStateMap[it.id]?.toggleableState == ToggleableState.On
              }
            ) {
              toggleableState = ToggleableState.On
            }
            syncLocationStateMap[parent.id] =
              SyncLocationState(parent.id, parent.parent?.id, toggleableState)
            parent = parent.parent
          }

          updateNestedCheckboxState(
            currentTreeNode = currentTreeNode,
            syncLocationStateMap = syncLocationStateMap,
            checked = checked.value,
          )
          onChecked()
        },
        modifier = Modifier.padding(0.dp),
        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary),
        interactionSource = remember { MutableInteractionSource() },
      )
      content(currentTreeNode)
    }
  }
}

/**
 * This function selects/deselects all the children for the [currentTreeNode] based on the value for
 * the [checked] parameter. The states for the [MultiSelectCheckbox] is updated in the
 * [syncLocationStateMap].
 */
fun <T> updateNestedCheckboxState(
  currentTreeNode: TreeNode<T>,
  syncLocationStateMap: MutableMap<String, SyncLocationState>,
  checked: Boolean,
) {
  val treeNodeArrayDeque = ArrayDeque(currentTreeNode.children)
  while (treeNodeArrayDeque.isNotEmpty()) {
    val currentNode = treeNodeArrayDeque.removeFirst()
    syncLocationStateMap[currentNode.id] =
      SyncLocationState(
        locationId = currentNode.id,
        parentLocationId = currentNode.parent?.id,
        toggleableState = ToggleableState(checked),
      )
    currentNode.children.forEach { treeNodeArrayDeque.addLast(it) }
  }
}
