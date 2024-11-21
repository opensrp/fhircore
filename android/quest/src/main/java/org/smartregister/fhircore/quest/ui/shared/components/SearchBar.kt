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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val SEARCH_BAR_TRAILING_ICON_TEST_TAG = "searchBarTrailingIconTestTag"
const val SEARCH_BAR_TRAILING_ICON_BUTTON_TEST_TAG = "searchBarTrailingIconButtonTestTag"
const val SEARCH_BAR_TRAILING_TEXT_FIELD_TEST_TAG = "searchBarTrailingTextFieldTestTag"

@Composable
fun SearchBar(
  onTextChanged: (String) -> Unit,
  onBackPress: () -> Unit,
  searchTextState: MutableState<TextFieldValue>,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.background(color = Color.White)) {
    TextField(
      value = searchTextState.value,
      onValueChange = { value ->
        searchTextState.value = value
        onTextChanged(value.text)
      },
      modifier = modifier.fillMaxWidth().testTag(SEARCH_BAR_TRAILING_TEXT_FIELD_TEST_TAG),
      textStyle = TextStyle(fontSize = 18.sp),
      leadingIcon = {
        IconButton(onClick = onBackPress) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = modifier.padding(16.dp),
          )
        }
      },
      trailingIcon = {
        if (searchTextState.value.text.isNotEmpty()) {
          // TODO: Enhancement add icon for recording audio input
          IconButton(
            onClick = {
              // Remove text from TextField when you press the 'X' icon
              searchTextState.value = TextFieldValue("")
              onTextChanged(searchTextState.value.text)
            },
            modifier = modifier.testTag(SEARCH_BAR_TRAILING_ICON_BUTTON_TEST_TAG),
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "",
              modifier =
                modifier.padding(16.dp).size(24.dp).testTag(SEARCH_BAR_TRAILING_ICON_TEST_TAG),
            )
          }
        }
      },
      singleLine = true,
      shape = RectangleShape,
      colors =
        TextFieldDefaults.textFieldColors(
          backgroundColor = Color.White,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent,
        ),
      placeholder = { SearchHint(modifier) },
    )
  }
}

@Composable
fun SearchHint(modifier: Modifier) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.wrapContentHeight().focusable(false).then(modifier),
  ) {
    Text(color = Color(0xff757575), text = stringResource(id = R.string.search_hint))
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun SearchBarWithSearchStateTextPreview() {
  val searchTextState = remember { mutableStateOf(TextFieldValue(text = "Jack")) }
  SearchBar(onTextChanged = {}, onBackPress = {}, searchTextState = searchTextState)
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun SearchBarWithEmptySearchTextStatePreview() {
  val searchTextState = remember { mutableStateOf(TextFieldValue(text = "")) }
  SearchBar(onTextChanged = {}, onBackPress = {}, searchTextState = searchTextState)
}
