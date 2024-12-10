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

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.datastore.dataFilterLocationIdsProtoStore
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewConfig
import org.smartregister.fhircore.engine.domain.model.SyncLocationState
import org.smartregister.fhircore.engine.ui.multiselect.TreeBuilder
import org.smartregister.fhircore.engine.ui.multiselect.TreeNode
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.retrieveRelatedEntitySyncLocationState
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

@HiltViewModel
class MultiSelectViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val fhirPathDataExtractor: FhirPathDataExtractor,
) : ViewModel() {

  val searchTextState: MutableState<String> = mutableStateOf("")
  val rootTreeNodes: SnapshotStateList<TreeNode<String>> = SnapshotStateList()
  val selectedNodes: SnapshotStateMap<String, SyncLocationState> = SnapshotStateMap()
  val isLoading = MutableLiveData(false)
  private var _rootTreeNodes: List<TreeNode<String>> = mutableListOf()

  fun populateLookupMap(context: Context, multiSelectViewConfig: MultiSelectViewConfig) {
    viewModelScope.launch {
      isLoading.postValue(true)
      // Populate previously selected nodes for every Multi-Select view action
      multiSelectViewConfig.viewActions.forEach {
        val previouslySelectedNodes =
          context.retrieveRelatedEntitySyncLocationState(
            multiSelectViewAction = it,
            filterToggleableStateOn = false,
          )
        previouslySelectedNodes.forEach { syncLocationState ->
          selectedNodes[syncLocationState.locationId] = syncLocationState
        }
      }

      val repositoryResourceDataList =
        defaultRepository
          .searchNestedResources(
            baseResourceIds = null,
            fhirResourceConfig = multiSelectViewConfig.resourceConfig,
            configComputedRuleValues = emptyMap(),
            activeResourceFilters = null,
            filterByRelatedEntityLocationMetaTag = false,
            currentPage = null,
            pageSize = null,
          )
          .values

      val resourcesMap =
        repositoryResourceDataList.associateByTo(
          mutableMapOf(),
          { it.resource.logicalId },
          { it.resource },
        )
      val rootNodeIds = mutableSetOf<String>()

      val lookupItems: List<TreeNode<String>> =
        resourcesMap.values.map { resource ->
          val parentId =
            fhirPathDataExtractor
              .extractValue(
                resource,
                multiSelectViewConfig.parentIdFhirPathExpression,
              )
              .extractLogicalIdUuid()
          val data =
            fhirPathDataExtractor
              .extractValue(
                resource,
                multiSelectViewConfig.contentFhirPathExpression,
              )
              .extractLogicalIdUuid()
          val isRootNode =
            fhirPathDataExtractor
              .extractValue(
                resource,
                multiSelectViewConfig.rootNodeFhirPathExpression.key,
              )
              .equals(multiSelectViewConfig.rootNodeFhirPathExpression.value, ignoreCase = true)
          if (isRootNode) {
            rootNodeIds.add(resource.logicalId)
          }

          val parentResource = resourcesMap[parentId]
          TreeNode(
            id = resource.logicalId,
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
      isLoading.postValue(false)
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

  suspend fun saveSelectedLocations(
    context: Context,
    viewActions: List<MultiSelectViewAction>,
    onSaveDone: () -> Unit,
  ) {
    try {
      viewActions.forEach {
        when (it) {
          MultiSelectViewAction.SYNC_DATA ->
            context.syncLocationIdsProtoStore.updateData { selectedNodes }
          MultiSelectViewAction.FILTER_DATA ->
            context.dataFilterLocationIdsProtoStore.updateData { selectedNodes }
        }
      }

      onSaveDone()
    } catch (ioException: IOException) {
      Timber.e("Error saving selected locations", ioException)
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
        val treeNodeArrayDeque = ArrayDeque(rootTreeNode.children)
        while (treeNodeArrayDeque.isNotEmpty()) {
          val currentNode = treeNodeArrayDeque.removeFirst()
          if (currentNode.data.contains(other = searchTerm, ignoreCase = true)) {
            when {
              rootTreeNodeMap.containsKey(rootTreeNode.id) -> return@forEach
              else -> {
                rootTreeNodeMap[rootTreeNode.id] = rootTreeNode
                return@forEach
              }
            }
          }
          currentNode.children.forEach { treeNodeArrayDeque.addLast(it) }
        }
      }
      rootTreeNodes.addAll(rootTreeNodeMap.values)
    }
  }
}
