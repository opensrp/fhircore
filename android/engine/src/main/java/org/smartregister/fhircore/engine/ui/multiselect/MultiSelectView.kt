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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TriStateCheckbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.LinkedList
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@Composable
fun <T> ColumnScope.MultiSelectView(
  rootNodeId: String,
  treeNodeMap: Map<String, TreeNode<T>>,
  selectedNodes: MutableMap<String, ToggleableState>,
  depth: Int = 0,
  content: @Composable (TreeNode<T>) -> Unit,
) {
  if (treeNodeMap.isEmpty()) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(id = R.string.no_data),
        modifier = Modifier.padding(16.dp),
        fontWeight = FontWeight.Bold,
      )
    }
  } else {
    val collapsedState = remember { mutableStateOf(false) }
    MultiSelectCheckbox(
      selectedNodes = selectedNodes,
      treeNodeMap = treeNodeMap,
      currentTreeNode = treeNodeMap.getValue(rootNodeId),
      depth = depth,
      content = content,
      collapsedState = collapsedState,
    )
    if (collapsedState.value) {
      treeNodeMap.getValue(rootNodeId).children.forEach {
        MultiSelectView(
          rootNodeId = it.id,
          treeNodeMap = treeNodeMap,
          selectedNodes = selectedNodes,
          depth = depth + 16,
          content = content,
        )
      }
    }
  }
}

@Composable
fun <T> MultiSelectCheckbox(
  selectedNodes: MutableMap<String, ToggleableState>,
  treeNodeMap: Map<String, TreeNode<T>>,
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
          var parentId = currentTreeNode.parentId
          while (!parentId.isNullOrEmpty()) {
            toggleableState = ToggleableState.Indeterminate
            val parentNode = treeNodeMap[parentId]
            if (
              parentNode?.children?.all {
                selectedNodes[it.id] == ToggleableState.Off || selectedNodes[it.id] == null
              } == true
            ) {
              toggleableState = ToggleableState.Off
            }
            if (parentNode?.children?.all { selectedNodes[it.id] == ToggleableState.On } == true) {
              toggleableState = ToggleableState.On
            }
            selectedNodes[parentId] = toggleableState
            parentId = treeNodeMap[parentId]?.parentId
          }

          // Select all the nested checkboxes
          val linkedList =
            currentTreeNode.children.map { treeNodeMap.getValue(it.id) }.let { LinkedList(it) }

          while (!linkedList.isEmpty()) {
            val currentNode = linkedList.removeFirst()
            selectedNodes[currentNode.id] = ToggleableState(checked.value)
            currentNode.children.forEach { linkedList.add(treeNodeMap.getValue(it.id)) }
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

@Preview(showBackground = true)
@Composable
fun MultiSelectViewNoDataPreview() {
  AppTheme {
    Column {
      MultiSelectView(
        selectedNodes = SnapshotStateMap(),
        treeNodeMap = SnapshotStateMap<String, TreeNode<String>>(),
        rootNodeId = "kenya",
        depth = 0,
        content = { treeNode -> Column { Text(text = treeNode.data) } },
      )
    }
  }
}
