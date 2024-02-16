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

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Demo_ExposedDropdownMenuBox(
  names: Collection<String>,
  selectedName: String?,
  onNameSelect: (String, Context) -> Unit,
) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }
  Box(
    modifier = Modifier.fillMaxWidth().padding(32.dp),
  ) {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
    ) {
      TextField(
        value = selectedName ?: "",
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        names.forEach { item ->
          DropdownMenuItem(
            text = { Text(text = item) },
            onClick = {
              onNameSelect(item, context)
              expanded = false
              Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
            },
          )
        }
      }
    }
  }
}
