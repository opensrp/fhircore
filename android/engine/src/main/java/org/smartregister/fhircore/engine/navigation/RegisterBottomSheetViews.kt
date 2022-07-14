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

package org.smartregister.fhircore.engine.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun RegisterBottomSheet(
  registers: List<RegisterBottomSheetItem>?,
  itemListener: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
    Column(modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)) {
      Text(
        text = stringResource(R.string.other_patients),
        textAlign = TextAlign.Start,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier =
        modifier
          .padding(horizontal = 12.dp, vertical = 16.dp)
          .align(Alignment.Start)
      )
      Divider(color = DividerColor, thickness = 1.dp)
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(registers!!) { index, item ->
          RegisterListItem(item, itemListener)
          if (index < registers.lastIndex)
            Divider(color = DividerColor, thickness = 1.dp)
        }
      }
    }
  }
}

@Composable
fun RegisterListItem(
  registerItem: RegisterBottomSheetItem,
  itemListener: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier =
    modifier
      .fillMaxWidth()
      .clickable { itemListener(registerItem.id) }
      .padding(14.dp)
  ) {
    Box(modifier = modifier.wrapContentWidth()) {}
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = registerItem.display, modifier = modifier.padding(horizontal = 12.dp))
      Text(
        text = "1", textAlign = TextAlign.Start,
        color = StatusTextColor,
        fontSize = 13.sp,
      )
    }
  }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun RegisterListItemPreview() {
  RegisterListItem(
    registerItem = RegisterBottomSheetItem("TestFragmentTag", "All Clients", true),
    itemListener = {}
  )
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun RegisterBottomSheetPreview() {
  RegisterBottomSheet(
    itemListener = {},
    registers =
      listOf(
        RegisterBottomSheetItem(id = "TestFragmentTag", display = "All Clients"),
        RegisterBottomSheetItem(id = "TestFragmentTag2", display = "Families", showCount = true)
      )
  )
}
