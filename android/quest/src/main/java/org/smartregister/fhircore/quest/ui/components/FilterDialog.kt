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

package org.smartregister.fhircore.quest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.smartregister.fhircore.quest.R

@Composable
fun FilterDialog(
  onFiltersApply: () -> Unit,
  onDismissAction: () -> Unit,
  hasActiveFilters: Boolean,
  clearFilter: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Dialog(
    onDismissRequest = onDismissAction,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().wrapContentHeight(),
    ) {
      Column(modifier = Modifier.padding(8.dp)) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(id = R.string.filters).uppercase(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.primary,
          )
          IconButton(onClick = onDismissAction) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          content()
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement =
            if (hasActiveFilters) Arrangement.SpaceBetween else Arrangement.End,
        ) {
          if (hasActiveFilters) {
            TextButton(
              onClick = {
                clearFilter()
                onDismissAction()
              },
              modifier = Modifier.wrapContentWidth(),
              colors =
                ButtonDefaults.textButtonColors(
                  contentColor = Color.Blue.copy(alpha = ContentAlpha.medium),
                ),
            ) {
              Text(text = stringResource(id = R.string.clear_filter).uppercase())
            }
          }

          TextButton(
            onClick = onFiltersApply,
            modifier = Modifier.wrapContentWidth(),
          ) {
            Text(text = stringResource(id = R.string.apply).uppercase())
          }
        }
      }
    }
  }
}
