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

package org.smartregister.fhircore.anc.util.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
fun BottomSheetListView(
    bottomSheetHolder: BottomSheetHolder,
    onBottomSheetListener: BottomSheetListDialog.OnClickedListItems,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = bottomSheetHolder.title,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                )
                val iconId = R.drawable.ic_close
                Image(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = modifier
                )
            }
            Divider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 12.dp, vertical = 18.dp)
                    .background(
                        color = colorResource(id = R.color.background_warning),
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_alert_triangle),
                    contentDescription = null,
                    modifier = modifier.padding(horizontal = 12.dp)
                )
                Text(
                    text = bottomSheetHolder.tvWarningTitle,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    modifier = modifier.padding(vertical = 12.dp)
                )
            }
            Text(
                text = bottomSheetHolder.subTitle,
                modifier = modifier.padding(horizontal = 12.dp),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = bottomSheetHolder.list,
                    itemContent = {
                        BottomListItem(it, onBottomSheetListener)
                        Divider(color = DividerColor, thickness = 1.dp)
                    }
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                TextButton(
                    onClick = { onBottomSheetListener.onCancel()},
                    modifier = modifier,


                ) {
                    Text(
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.black),
                        text = stringResource(id = R.string.cancel),
                    )
                }
                TextButton(
                    enabled = bottomSheetHolder.list.find { it.selected }?.selected!!,
                    onClick = {  },
                    modifier = modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.colorPrimary),
                        text = stringResource(id = R.string.str_save),
                    )
                }
            }
        }
    }
}

@Composable
fun BottomListItem(
    model: BottomSheetDataModel,
    onBottomSheetListener: BottomSheetListDialog.OnClickedListItems,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clickable { onBottomSheetListener.onItemClicked(model.id) }
            .padding(14.dp)
    ) {
        Box(modifier = modifier.wrapContentWidth()) {
            if (model.selected) {
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
        Text(text = model.itemName, modifier = modifier.padding(horizontal = 12.dp))
    }
}


@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun SimpleRadioButtonComponent() {
    val radioOptions = listOf("DSA", "Java", "C++")
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[2]) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) }
                        )
                        .padding(horizontal = 16.dp)
                ) {
                    RadioButton(
                        selected = (text == selectedOption),modifier = Modifier.padding(all = Dp(value = 8F)),
                        onClick = {
                            onOptionSelected(text)
                        }
                    )
                    Text(
                        text = text,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun RegisterBottomSheetPreview() {
    BottomSheetListView(
        bottomSheetHolder = BottomSheetHolder(
            stringResource(id = R.string.label_assign_new_family_head),
            stringResource(id = R.string.label_select_new_head),
            stringResource(id = R.string.label_remove_family_warning),
            listOf(
                BottomSheetDataModel("TestFragmentTag", "All Clients", "1241", true),
                BottomSheetDataModel("TestFragmentTag", "All Clients", "1241")
            )

        ),
        onBottomSheetListener = object : BottomSheetListDialog.OnClickedListItems {
            override fun onSave(bottomSheetDataModel: BottomSheetDataModel) {
                TODO("Not yet implemented")
            }

            override fun onCancel() {
                TODO("Not yet implemented")
            }

            override fun onItemClicked(id: String) {
                TODO("Not yet implemented")
            }

        }
    )
}

