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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
  progressMessage: String = "",
) {
  LazyColumn(modifier = modifier) {
    items(pagingItems, key = { it.logicalId }) {
      RegisterRowItem(registerViewData = it!!, onRowClick = onRowClick)
    }
    pagingItems.apply {
      val finishedLoading =
        loadState.refresh !is LoadState.Loading &&
          loadState.prepend !is LoadState.Loading &&
          loadState.append !is LoadState.Loading

      if (itemCount == 0 && finishedLoading) {
        item {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(14.dp),
          ) {
            Box(
              modifier =
                Modifier.background(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                  )
                  .padding(24.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.PersonSearch,
                contentDescription = "",
                tint = Color.LightGray,
                modifier = Modifier.size(72.dp).align(Alignment.Center),
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "List empty, nothing to show here", style = MaterialTheme.typography.h6)
          }
        }
      }
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
              onClickRetry = { retry() },
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
      text = progressMessage,
    )
  }
}

@Composable
fun RegisterRowItem(registerViewData: RegisterViewData, onRowClick: (String) -> Unit) {
  when (registerViewData.registerType) {
    RegisterData.HivRegisterData::class,
    RegisterData.TracingRegisterData::class,
    RegisterData.AppointmentRegisterData::class, -> {
      HivPatientRegisterListRow(data = registerViewData, onItemClick = onRowClick)
    }
    else -> {
      RegisterListRow(registerViewData = registerViewData, onRowClick = onRowClick)
      Divider(color = DividerColor, thickness = 1.dp)
    }
  }
}
