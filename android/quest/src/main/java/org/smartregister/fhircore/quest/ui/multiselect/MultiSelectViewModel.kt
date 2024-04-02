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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewConfig
import org.smartregister.fhircore.engine.ui.multiselect.TreeBuilder
import org.smartregister.fhircore.engine.ui.multiselect.TreeNode
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltViewModel
class MultiSelectViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val preferenceDataStore: PreferenceDataStore,
) : ViewModel() {

  val searchTextState: MutableState<String> = mutableStateOf("")
  val rootTreeNodes: SnapshotStateList<TreeNode<String>> = SnapshotStateList()
  val selectedNodes: SnapshotStateMap<String, ToggleableState> = SnapshotStateMap()
  val flag = MutableLiveData(false)
  private var _rootTreeNodes: List<TreeNode<String>> = mutableListOf()

  fun populateLookupMap(multiSelectViewConfig: MultiSelectViewConfig) {
    // Mark previously selected nodes
    viewModelScope.launch {
      flag.postValue(true)
      val previouslySelectedNodes =
        preferenceDataStore.read(PreferenceDataStore.SYNC_LOCATION_IDS).firstOrNull()
      if (!previouslySelectedNodes.isNullOrEmpty()) {
        previouslySelectedNodes
          .split(",")
          .asSequence()
          .map { it.split(":") }
          .filter { it.size == 2 }
          .map { Pair(it.first(), it.last()) }
          .forEach { selectedNodes[it.first] = ToggleableState.valueOf(it.second) }
      }

      val resourcesMap =
        defaultRepository
          .searchResourcesRecursively(
            fhirResourceConfig = multiSelectViewConfig.resourceConfig,
            filterActiveResources = null,
            secondaryResourceConfigs = null,
            configRules = null,
          )
          .associateByTo(mutableMapOf(), { it.resource.logicalId }, { it.resource })
      val rootNodeIds = mutableSetOf<String>()

      val lookupItems: List<TreeNode<String>> =
        resourcesMap.values.map {
          val parentId =
            fhirPathDataExtractor
              .extractValue(
                it,
                multiSelectViewConfig.parentIdFhirPathExpression,
              )
              .extractLogicalIdUuid()
          val data =
            fhirPathDataExtractor
              .extractValue(
                it,
                multiSelectViewConfig.contentFhirPathExpression,
              )
              .extractLogicalIdUuid()
          // TODO use configuration to obtain root nodes
          if (parentId.isEmpty()) {
            rootNodeIds.add(it.logicalId)
          }

          val parentResource = resourcesMap[parentId]

          TreeNode(
            id = it.logicalId,
            parent =
              if (parentResource != null) {
                TreeNode(
                  id = parentResource.logicalId,
                  parent = null,
                  data =
                    fhirPathDataExtractor
                      .extractValue(
                        parentResource,
                        multiSelectViewConfig.contentFhirPathExpression,
                      )
                      .extractLogicalIdUuid(),
                )
              } else {
                null
              },
            data = data,
          )
        }
      flag.postValue(false)
      _rootTreeNodes = TreeBuilder.buildTrees(lookupItems, rootNodeIds)
      rootTreeNodes.addAll(_rootTreeNodes)
    }
  }

  fun onTextChanged(searchTerm: String) {
    searchTextState.value = searchTerm
    if (searchTerm.isEmpty() || searchTerm.isBlank()) {
      rootTreeNodes.run {
        clear()
        addAll(_rootTreeNodes)
      }
    }
  }

  fun onSelectionDone(dismiss: () -> Unit) {
    viewModelScope.launch {
      // TODO Consider using a proto-datastore here
      preferenceDataStore.write(
        PreferenceDataStore.SYNC_LOCATION_IDS,
        selectedNodes.map { "${it.key}:${it.value}" }.joinToString(","),
      )
      dismiss()
    }
  }

  fun search() {
    val searchTerm = searchTextState.value
    if (searchTerm.isNotEmpty() && searchTerm.isNotEmpty()) {
      val rootTreeNodeMap = mutableMapOf<String, TreeNode<String>>()
      rootTreeNodes.clear()
      _rootTreeNodes.forEach { rootTreeNode ->
        if (
          rootTreeNode.data.contains(searchTerm, true) &&
            !rootTreeNodeMap.containsKey(rootTreeNode.id)
        ) {
          rootTreeNodeMap[rootTreeNode.id] = rootTreeNode
          return@forEach
        }
        val childrenList = LinkedList(rootTreeNode.children)
        while (childrenList.isNotEmpty()) {
          val currentNode = childrenList.removeFirst()
          if (currentNode.data.contains(other = searchTerm, ignoreCase = true)) {
            when {
              rootTreeNodeMap.containsKey(rootTreeNode.id) -> return@forEach
              else -> {
                rootTreeNodeMap[rootTreeNode.id] = rootTreeNode
                return@forEach
              }
            }
          }
          currentNode.children.forEach { childrenList.add(it) }
        }
      }
      rootTreeNodes.addAll(rootTreeNodeMap.values)
    }
  }
}
