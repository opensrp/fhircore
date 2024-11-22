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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.domain.model.SyncLocationState
import org.smartregister.fhircore.engine.ui.multiselect.MultiSelectView
import org.smartregister.fhircore.engine.ui.multiselect.TreeNode
import org.smartregister.fhircore.engine.ui.multiselect.updateNestedCheckboxState
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.extension.isIn

@Composable
fun MultiSelectBottomSheetView(
  rootTreeNodes: SnapshotStateList<TreeNode<String>>,
  syncLocationStateMap: MutableMap<String, SyncLocationState>,
  title: String?,
  onDismiss: () -> Unit,
  searchTextState: MutableState<String>,
  onSearchTextChanged: (String) -> Unit,
  onSelectionDone: (List<MultiSelectViewAction>) -> Unit,
  search: () -> Unit,
  isLoading: State<Boolean?>,
  multiSelectViewAction: List<MultiSelectViewAction>,
  mutuallyExclusive: Boolean,
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  Scaffold(
    topBar = {
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
        OutlinedTextField(
          value = searchTextState.value,
          onValueChange = { value -> onSearchTextChanged(value) },
          modifier =
            Modifier.background(color = Color.Transparent)
              .padding(vertical = 16.dp, horizontal = 8.dp)
              .fillMaxWidth(),
          textStyle = TextStyle(fontSize = 18.sp),
          trailingIcon = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp),
            ) {
              if (searchTextState.value.isNotEmpty()) {
                IconButton(
                  onClick = {
                    keyboardController?.hide()
                    search()
                  },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "",
                  )
                }
                IconButton(onClick = { onSearchTextChanged("") }, modifier = Modifier.size(28.dp)) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "",
                  )
                }
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
          keyboardActions =
            KeyboardActions(
              onSearch = {
                keyboardController?.hide()
                search()
              },
            ),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        )
      }
    },
    bottomBar = {
      if (syncLocationStateMap.isNotEmpty() && rootTreeNodes.isNotEmpty()) {
        Button(
          onClick = { onSelectionDone(multiSelectViewAction) },
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
        ) {
          Text(
            text =
              stringResource(
                  id =
                    when (multiSelectViewAction.first()) {
                      MultiSelectViewAction.SYNC_DATA -> R.string.sync_data
                      MultiSelectViewAction.FILTER_DATA -> R.string.apply_filter
                    },
                )
                .uppercase(),
            modifier = Modifier.padding(8.dp),
          )
        }
      }
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier.fillMaxSize().padding(paddingValues),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (isLoading.value == true) {
        CircularProgressIndicator(
          color = MaterialTheme.colors.primary,
        )
      }
      if (isLoading.value == false && rootTreeNodes.isEmpty()) {
        Text(text = stringResource(R.string.no_results))
      } else {
        LazyColumn(
          modifier = Modifier.padding(horizontal = 8.dp),
        ) {
          items(rootTreeNodes, key = { item -> item.id }) { rootTreeNode ->
            Column {
              MultiSelectView(
                rootTreeNode = rootTreeNode,
                syncLocationStateMap = syncLocationStateMap,
                onChecked = {
                  if (mutuallyExclusive) {
                    rootTreeNodes.forEach { currentNode ->
                      val currentNodeToggleableState =
                        syncLocationStateMap[currentNode.id]?.toggleableState
                      if (
                        currentNode.id != rootTreeNode.id &&
                          currentNodeToggleableState != null &&
                          currentNodeToggleableState.isIn(
                            ToggleableState.On,
                            ToggleableState.Indeterminate,
                          )
                      ) {
                        // De-select the root and its children
                        syncLocationStateMap[currentNode.id] =
                          SyncLocationState(
                            locationId = currentNode.id,
                            parentLocationId = currentNode.parent?.id,
                            toggleableState = ToggleableState.Off,
                          )

                        updateNestedCheckboxState(
                          currentTreeNode = currentNode,
                          syncLocationStateMap = syncLocationStateMap,
                          checked = false,
                        )
                      }
                    }
                  }
                },
              ) { treeNode ->
                Column { Text(text = treeNode.data) }
              }
            }
          }
        }
      }
    }
  }
}
