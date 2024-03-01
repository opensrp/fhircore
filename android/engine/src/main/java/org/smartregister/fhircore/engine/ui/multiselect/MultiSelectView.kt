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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.LinkedList

@Stable
class TreeNode<T>(
  val id: String,
  val data: T,
  val children: List<TreeNode<T>>? = null,
)

@Composable
fun <T> ColumnScope.MultiSelectView(
  treeNode: TreeNode<T>,
  selectedNodes: SnapshotStateMap<String, Boolean>,
  depth: Int,
  content: @Composable (TreeNode<T>) -> Unit,
) {
  val collapsedState = remember { mutableStateOf(false) }
  MultiSelectCheckbox(
    selectedNodes = selectedNodes,
    treeNode = treeNode,
    depth = depth,
    content = content,
    collapsedState = collapsedState
  )

  if (collapsedState.value) {
    treeNode.children?.forEach {
      MultiSelectView(
        treeNode = it,
        selectedNodes = selectedNodes,
        depth = depth + 16,
        content = content,
      )
    }
  }
}

@Composable
fun <T> MultiSelectCheckbox(
  selectedNodes: SnapshotStateMap<String, Boolean>,
  treeNode: TreeNode<T>,
  depth: Int,
  content: @Composable (TreeNode<T>) -> Unit,
  collapsedState: MutableState<Boolean>,
) {
  Column {
    Row(
      modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = depth.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!treeNode.children.isNullOrEmpty()) {
        Icon(
          imageVector =
          if (collapsedState.value) Icons.Default.ArrowDropDown else
            Icons.AutoMirrored.Filled.ArrowRight,
          contentDescription = null,
          tint = Color.Gray,
          modifier = Modifier.clickable {
            collapsedState.value = !collapsedState.value
          }
        )
      }
      Checkbox(
        checked = selectedNodes[treeNode.id] ?: false,
        onCheckedChange = { checked ->
          selectedNodes[treeNode.id] = checked
          // Select all the nested checkboxes
          val linkedList = treeNode.children?.let { LinkedList(it) }

          while (!linkedList.isNullOrEmpty()) {
            val currentNode = linkedList.removeFirst()
            selectedNodes[currentNode.id] = checked
            currentNode.children?.let { linkedList.addAll(it) }
          }
        },
        modifier = Modifier.padding(0.dp),
      )
      content(treeNode)
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MultiSelectViewPreview() {
  Column {
    MultiSelectView(
      selectedNodes = SnapshotStateMap<String, Boolean>().apply { put("sampleId1.1", true) },
      treeNode =
      TreeNode(
        id = "sampleId1",
        data = "Root",
        children =
        listOf(
          TreeNode(
            id = "sampleId1.1",
            data = "Branch 1.1",
            children =
            listOf(
              TreeNode(
                id = "sampleId1.1.1",
                data = "Branch 1.1.1",
                children = listOf(TreeNode(id = "sampleId1.1.1.1", data = "Branch 1.1.1.1")),
              ),
            ),
          ),
          TreeNode(
            id = "sampleId3",
            data = "Branch 2",
            children =
            listOf(
              TreeNode(id = "sampleId2.1", data = "Branch 2.1"),
            ),
          ),
        ),
      ),
      depth = 0,
      content = { treeNode -> Text(text = treeNode.data) },
    )
  }
}
