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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun RegisterBottomSheet(
  registers: List<RegisterItem>,
  itemListener: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
      Text(
        text = stringResource(R.string.select_register),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier =
          modifier.padding(horizontal = 12.dp, vertical = 16.dp).align(Alignment.CenterHorizontally)
      )
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(
          items = registers,
          itemContent = {
            RegisterListItem(it, itemListener)
            Divider(color = DividerColor, thickness = 1.dp)
          }
        )
      }
    }
  }
}

@Composable
fun RegisterListItem(
  registerItem: RegisterItem,
  itemListener: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier =
      modifier.fillMaxWidth().clickable { itemListener(registerItem.uniqueTag) }.padding(14.dp)
  ) {
    Box(modifier = modifier.wrapContentWidth()) {
      if (registerItem.isSelected) {
        Image(
          painter = painterResource(R.drawable.ic_green_tick),
          contentDescription = stringResource(id = R.string.tick),
          colorFilter = ColorFilter.tint(color = Color.Gray),
          modifier = modifier.size(22.dp)
        )
      } else {
        Spacer(modifier = modifier.width(20.dp))
      }
    }
    Text(text = registerItem.title, modifier = modifier.padding(horizontal = 12.dp))
  }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun RegisterListItemPreview() {
  RegisterListItem(
    registerItem = RegisterItem("TestFragmentTag", "All Clients", true),
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
        RegisterItem(uniqueTag = "TestFragmentTag", title = "All Clients"),
        RegisterItem(uniqueTag = "TestFragmentTag2", title = "Families", isSelected = true)
      )
  )
}
