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

package org.smartregister.fhircore.quest.ui.patient.register.components

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
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.ui.patient.register.PatientRowClickListenerIntent

@Composable
fun PatientRegisterList(
  pagingItems: LazyPagingItems<PatientItem>,
  modifier: Modifier = Modifier,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit
) {
  LazyColumn {
    items(pagingItems, key = { it.id }) {
      PatientRow(it!!, clickListener, modifier = modifier)
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
            ErrorMessage(message = loadStateError.error.message ?: "", onClickRetry = { retry() })
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
fun dummyPatientPagingList(): LazyPagingItems<PatientItem> {
  val listFlow =
    flowOf(
      PagingData.from(
        listOf(
          PatientItem(
            id = "my-test-id1",
            identifier = "10001",
            name = "John Doe",
            gender = "M",
            age = "27y",
            displayAddress = "Nairobi"
          ),
          PatientItem(
            id = "my-test-id2",
            identifier = "10002",
            name = "Jane Doe",
            gender = "F",
            age = "20y",
            displayAddress = "Nairobi"
          )
        )
      )
    )
  return listFlow.collectAsLazyPagingItems()
}
