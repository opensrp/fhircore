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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.fhir.sync.State
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor

const val DRAWER_MENU = "Drawer Menu"
const val SEARCH = "Search"
const val CLEAR = "Clear"
const val TITLE_ROW_TEST_TAG = "titleRowTestTag"
const val TOP_ROW_ICON_TEST_TAG = "topRowIconTestTag"
const val TOP_ROW_TEXT_TEST_TAG = "topRowTextTestTag"
const val OUTLINED_BOX_TEST_TAG = "outlinedBoxTestTag"
const val TRAILING_ICON_TEST_TAG = "trailingIconTestTag"
const val TRAILING_ICON_BUTTON_TEST_TAG = "trailingIconButtonTestTag"
const val LEADING_ICON_TEST_TAG = "leadingIconTestTag"
const val SEARCH_FIELD_TEST_TAG = "searchFieldTestTag"

@Composable
fun TopScreenSection(
  modifier: Modifier = Modifier,
  title: String,
  syncStateFlow: Flow<State>,
  searchText: String,
  searchPlaceholder: String? = null,
  onSearchTextChanged: (String) -> Unit,
  onTitleIconClick: () -> Unit
) {
  val syncState = syncStateFlow.collectAsState(initial = null)

  Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colors.primary)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(vertical = 8.dp).testTag(TITLE_ROW_TEST_TAG)
    ) {
      IconButton(onClick = onTitleIconClick) {
        Icon(
          Icons.Filled.Menu,
          contentDescription = DRAWER_MENU,
          tint = Color.White,
          modifier = modifier.testTag(TOP_ROW_ICON_TEST_TAG)
        )
      }
      Text(
        text = title,
        fontSize = 20.sp,
        color = Color.White,
        modifier = modifier.testTag(TOP_ROW_TEXT_TEST_TAG)
      )
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
          text = searchPlaceholder ?: stringResource(R.string.search_hint),
          modifier = modifier.testTag(SEARCH_FIELD_TEST_TAG),
        )
      },
      modifier =
        modifier
          .padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(size = 10.dp))
          .background(Color.White)
          .testTag(OUTLINED_BOX_TEST_TAG),
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          SEARCH,
          modifier = modifier.testTag(LEADING_ICON_TEST_TAG)
        )
      },
      trailingIcon = {
        if (searchText.isNotEmpty())
          IconButton(
            onClick = { onSearchTextChanged("") },
            modifier = modifier.testTag(TRAILING_ICON_BUTTON_TEST_TAG)
          ) {
            Icon(
              imageVector = Icons.Filled.Clear,
              CLEAR,
              tint = Color.Gray,
              modifier = modifier.testTag(TRAILING_ICON_TEST_TAG)
            )
          }
      }
    )

    AnimatedVisibility(visible = syncState.value?.isSyncEnded() == false) {
      Column(modifier = modifier.background(MaterialTheme.colors.background).padding(10.dp)) {
        LinearProgressIndicator(
          0.2f,
          modifier.fillMaxWidth().padding(vertical = 5.dp),
          MaterialTheme.colors.primary
        )

        Row {
          Text(
            text =
              if (syncState.value?.isSyncEnded() == true)
                syncState.value?.javaClass?.simpleName?.uppercase() ?: ""
              else stringResource(R.string.syncing),
            fontSize = 16.sp,
            color = MaterialTheme.colors.primary,
            modifier = modifier.testTag(TOP_ROW_TEXT_TEST_TAG)
          )
          Spacer(Modifier.weight(1f).padding(5.dp))
          Text(
            text = "${syncState.value.progressAction()} ${syncState.value.progressPercent()}",
            fontSize = 16.sp,
            color = MaterialTheme.colors.primary,
            modifier = modifier.testTag(TOP_ROW_TEXT_TEST_TAG)
          )
        }
      }
    }
  }
}

private fun State.isSyncEnded() = this is State.Finished || this is State.Failed

private fun State?.progressAction() =
  if (this is State.InProgress) "${this.syncOperation.name.lowercase()}ing ... " else ""

private fun State?.progressPercent() =
  if (this is State.InProgress) "${this.percentCompleted.times(100).roundToInt()}%" else ""

@Preview(showBackground = true)
@Composable
fun TopScreenSectionPreview() {
  TopScreenSection(
    title = "All Clients",
    searchText = "Eddy",
    onSearchTextChanged = {},
    syncStateFlow = flowOf()
  ) {}
}
