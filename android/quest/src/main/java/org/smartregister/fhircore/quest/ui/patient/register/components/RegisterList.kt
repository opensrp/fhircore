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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import timber.log.Timber

@Composable
fun RegisterList(
  pagingItems: LazyPagingItems<RegisterViewData>,
  onRowClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  progressMessage: String = ""
) {
  LazyColumn(modifier = modifier) {
    items(pagingItems, key = { it.logicalId }) {
      RegisterRowItem(registerViewData = it!!, onRowClick = onRowClick)
    }
    pagingItems.apply {
      when {
        loadState.refresh is LoadState.Loading ->
          item { BoxedCircularProgressBar(progressMessage = progressMessage) }
        loadState.append is LoadState.Loading ->
          item { BoxedCircularProgressBar(progressMessage = progressMessage) }
        loadState.refresh is LoadState.Error -> {
          val loadStateError = pagingItems.loadState.refresh as LoadState.Error
          item {
            ErrorMessage(
              message = loadStateError.error.also { Timber.e(it) }.localizedMessage!!,
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
fun BoxedCircularProgressBar(progressMessage: String) {
  Box(
    modifier = Modifier.padding(16.dp).fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressBar(
      modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally),
      text = progressMessage
    )
  }
}

@Composable
fun RegisterRowItem(registerViewData: RegisterViewData, onRowClick: (String) -> Unit) {
  when (registerViewData.registerType) {
    RegisterData.HivRegisterData::class,
    RegisterData.TracingRegisterData::class,
    RegisterData.AppointmentRegisterData::class -> {
      HivPatientRegisterListRow(data = registerViewData, onItemClick = onRowClick)
    }
    else -> {
      RegisterListRow(registerViewData = registerViewData, onRowClick = onRowClick)
      Divider(color = DividerColor, thickness = 1.dp)
    }
  }
}
