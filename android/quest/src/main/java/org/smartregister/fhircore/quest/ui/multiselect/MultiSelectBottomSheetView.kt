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

package org.smartregister.fhircore.quest.ui.multiselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.multiselect.MultiSelectView
import org.smartregister.fhircore.engine.ui.multiselect.TreeNode
import org.smartregister.fhircore.engine.ui.theme.DividerColor

@Composable
fun MultiSelectBottomSheetView(
  rootNodeIds: SnapshotStateList<String>,
  treeNodeMap: SnapshotStateMap<String, TreeNode<String>>,
  selectedNodes: SnapshotStateMap<String, ToggleableState>,
  title: String?,
  onDismiss: () -> Unit,
  searchTextState: MutableState<TextFieldValue>,
  onTextChanged: (String) -> Unit,
) {
  Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
      ) {
        Text(
          text = if (title.isNullOrEmpty()) stringResource(R.string.select_location) else title,
          textAlign = TextAlign.Start,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
        )
        Icon(
          imageVector = Icons.Filled.Clear,
          contentDescription = null,
          modifier = Modifier.clickable { onDismiss() },
        )
      }
      Divider(color = DividerColor, thickness = 1.dp)
      Box(
        modifier =
          Modifier.background(color = Color.Transparent)
            .padding(vertical = 16.dp, horizontal = 8.dp),
      ) {
        OutlinedTextField(
          value = searchTextState.value,
          onValueChange = { value ->
            searchTextState.value = value
            onTextChanged(value.text)
          },
          modifier = Modifier.fillMaxWidth(),
          textStyle = TextStyle(fontSize = 18.sp),
          trailingIcon = {
            if (searchTextState.value.text.isNotEmpty()) {
              IconButton(
                onClick = {
                  searchTextState.value = TextFieldValue("")
                  onTextChanged(searchTextState.value.text)
                },
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "",
                  modifier = Modifier.padding(16.dp).size(24.dp),
                )
              }
            }
          },
          singleLine = true,
          placeholder = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                color = Color(0xff757575),
                text = stringResource(id = R.string.search),
              )
            }
          },
        )
      }
      LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        items(rootNodeIds, key = { item -> item }) {
          MultiSelectView(
            rootNodeId = it,
            treeNodeMap = treeNodeMap,
            selectedNodes = selectedNodes,
          ) { treeNode ->
            Column { Text(text = treeNode.data) }
          }
        }

        item {
          Button(
            onClick = { /*TODO Get selected nodes*/},
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
          ) {
            Text(
              text = stringResource(id = R.string.sync_data).uppercase(),
              modifier = Modifier.padding(8.dp),
            )
          }
        }
      }
    }
  }
}
