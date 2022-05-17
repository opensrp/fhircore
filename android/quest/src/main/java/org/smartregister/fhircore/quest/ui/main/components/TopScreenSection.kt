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

package org.smartregister.fhircore.quest.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor

const val DRAWER_MENU = "Drawer Menu"
const val SEARCH = "Search"
const val CLEAR = "Clear"

@Composable
fun TopScreenSection(
  modifier: Modifier = Modifier,
  title: String,
  searchText: String,
  onSearchTextChanged: (String) -> Unit,
  onTitleIconClick: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(vertical = 8.dp)
    ) {
      IconButton(onClick = onTitleIconClick) {
        Icon(Icons.Filled.Menu, contentDescription = DRAWER_MENU, tint = Color.White)
      }
      Text(text = title, fontSize = 20.sp, color = Color.White)
    }
    OutlinedTextField(
      colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.DarkGray),
      value = searchText,
      onValueChange = { onSearchTextChanged(it) },
      maxLines = 1,
      singleLine = true,
      placeholder = {
        Text(
          color = GreyTextColor,
          text = stringResource(R.string.search_hint),
        )
      },
      modifier =
        modifier
          .padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(size = 10.dp))
          .background(Color.White),
      leadingIcon = { Icon(imageVector = Icons.Filled.Search, SEARCH) },
      trailingIcon = {
        if (searchText.isNotEmpty())
          IconButton(onClick = { onSearchTextChanged("") }) {
            Icon(imageVector = Icons.Filled.Clear, CLEAR, tint = Color.Gray)
          }
      }
    )
  }
}

@Preview(showBackground = true)
@Composable
fun TopScreenSectionPreview() {
  TopScreenSection(title = "All Clients", searchText = "Eddy", onSearchTextChanged = {}) {}
}
