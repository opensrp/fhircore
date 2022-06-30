/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.task.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.flowOf
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.data.task.model.PatientTaskItem
import org.smartregister.fhircore.quest.ui.task.PatientTaskListenerIntent

@Composable
fun PatientTaskList(
  pagingItems: LazyPagingItems<PatientTaskItem>,
  useLabel: Boolean,
  modifier: Modifier = Modifier,
  clickListener: (PatientTaskListenerIntent, PatientTaskItem) -> Unit
) {

  LazyColumn {
    items(pagingItems, key = { it.id }) {
      PatientTaskRow(it!!, useLabel, clickListener, modifier = modifier)
      Divider(color = DividerColor, thickness = 1.dp)
    }

    pagingItems.apply {
      when {
        loadState.refresh is LoadState.Loading ->
          item { CircularProgressBar(modifier = modifier.fillParentMaxSize()) }
        loadState.append is LoadState.Loading -> item { CircularProgressBar() }
        loadState.refresh is LoadState.Error -> {
          val loadStateError = pagingItems.loadState.refresh as LoadState.Error
          item {
            ErrorMessage(
              message = loadStateError.error.localizedMessage!!,
              modifier = modifier.fillParentMaxSize(),
              onClickRetry = { retry() }
            )
          }
        }
        loadState.append is LoadState.Error -> {
          val error = pagingItems.loadState.append as LoadState.Error
          item {
            ErrorMessage(message = error.error.localizedMessage!!, onClickRetry = { retry() })
          }
        }
      }
    }
  }
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun dummyPatientTaskPagingList(): LazyPagingItems<PatientTaskItem> {
  val listFlow =
    flowOf(
      PagingData.from(
        listOf(
          PatientTaskItem(
            id = "1",
            name = "Eve",
            gender = "F",
            birthdate = "2020-03-10".getDate("yyyy-MM-dd"),
            address = "Nairobi",
            description = "Sick Visit",
            overdue = true
          ),
          PatientTaskItem(
            id = "2",
            name = "Vivi",
            gender = "M",
            birthdate = "2021-04-20".getDate("yyyy-MM-dd"),
            address = "Nairobi",
            description = "Immunization Visit",
            overdue = false
          )
        )
      )
    )
  return listFlow.collectAsLazyPagingItems()
}
