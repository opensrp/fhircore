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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
            onClick = {
              onNameSelect(item, context)
              expanded = false
              Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
            },
          ) {
            Text(text = item)
          }
        }
      }
    }
  }
}

@Composable
fun DropdownList(
  itemList: Collection<String>,
  selectedName: String?,
  onNameSelect: (String, Context) -> Unit,
) {
  val context = LocalContext.current
  var showDropdown by rememberSaveable { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // button
    Row(
      modifier =
        Modifier.align(Alignment.CenterHorizontally).clickable { showDropdown = !showDropdown },
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = selectedName ?: "",
        color = MaterialTheme.colors.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(vertical = 8.dp),
      )
      Icon(
        Icons.Filled.ArrowDropDown,
        "dropdown",
        tint = MaterialTheme.colors.primary.copy(alpha = 0.8f),
      )
    }

    // dropdown list
    Box() {
      if (showDropdown) {
        Column(
          modifier =
            Modifier.heightIn(max = 90.dp)
              .verticalScroll(state = scrollState)
              .border(width = 0.5.dp, color = Color.Gray),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          itemList.onEachIndexed { index, item ->
            if (index != 0) {
              Divider(thickness = 1.dp, color = Color.LightGray)
            }
            Box(
              modifier =
                Modifier.background(Color.White).fillMaxWidth().clickable {
                  onNameSelect(item, context)
                  showDropdown = !showDropdown
                },
              contentAlignment = Alignment.Center,
            ) {
              Text(text = item)
            }
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun previewDropDown() {
  DropdownList(itemList = listOf("yellow", "red", "green"), selectedName = "yellow") { _, _ -> }
}
