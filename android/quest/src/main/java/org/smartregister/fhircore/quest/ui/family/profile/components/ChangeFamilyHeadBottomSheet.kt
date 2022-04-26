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

package org.smartregister.fhircore.quest.ui.family.profile.components

import android.icu.text.CaseMap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData

const val TAG_SAVE = "save"
const val TAG_SELECTION = "selection"
const val TAG_CANCEL = "cancel"

@OptIn(ExperimentalMaterialApi::class)
@Composable fun ChangeFamilyHeadBottomSheet(coroutineScope: CoroutineScope,
                                            bottomSheetScaffoldState: BottomSheetScaffoldState,
                                            title: String,
                                            familyMembers: ProfileViewData.FamilyProfileViewData?,
                                            onSaveClick: (String) -> Unit,
                                            onCancelClick: () -> Unit,
                                            modifier: Modifier = Modifier) {

        var source by remember { mutableStateOf(familyMembers) }
        var isEnabled by remember { mutableStateOf(false) }
        Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                    text = title,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Light,
                    fontSize = 20.sp,
                    )
                    Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                    tint = DefaultColor.copy(0.8f),
                    modifier =
                    modifier.clickable {
                        coroutineScope.launch {
                            if (!bottomSheetScaffoldState.bottomSheetState.isCollapsed)
                                bottomSheetScaffoldState.bottomSheetState.collapse()
                        }
                    }
                    )
                }
                Divider()
                Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 12.dp, vertical = 18.dp)
                .background(
                color = colorResource(id = R.color.background_warning),
                shape =
                RoundedCornerShape(
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
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = modifier.padding(vertical = 12.dp)
                    )
                }
                Text(
                text = bottomSheetHolder.subTitle.uppercase(),
                modifier = modifier.padding(horizontal = 12.dp),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Light,
                fontSize = 18.sp,
                )
                LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                    items = source.list.toMutableStateList(),
                    itemContent = { index, item ->
                        BottomListItem(item) {
                            isEnabled = true
                            source.list.forEach { it.selected = false }
                            source.list[index].selected = true
                            source = source.copy(reselect = source.reselect.not())
                        }
                        Divider(color = DividerColor, thickness = 1.dp)
                    }
                    )
                }
                Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    TextButton(
                    onClick = { onBottomSheetListener.onCancel() },
                    modifier = modifier.fillMaxWidth().weight(1F).testTag(TAG_CANCEL),
                    ) {
                        Text(
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.black),
                        text = stringResource(id = R.string.cancel),
                        )
                    }
                    TextButton(
                    enabled = isEnabled,
                    onClick = { onBottomSheetListener.onSave(source.list.first { it.selected }) },
                    modifier = modifier.fillMaxWidth().weight(1F).testTag(TAG_SAVE),
                    colors =
                    ButtonDefaults.textButtonColors(
                    backgroundColor =
                    colorResource(id = if (isEnabled) R.color.colorPrimary else R.color.white)
                    )
                    ) {
                        Text(
                        fontSize = 14.sp,
                        color = colorResource(id = if (isEnabled) R.color.white else R.color.colorPrimary),
                        text = stringResource(id = R.string.str_save).uppercase(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun BottomListItem(
    model: BottomSheetDataModel,
    modifier: Modifier = Modifier,
    onClick: (BottomSheetDataModel) -> Unit
    ) {
        Row(modifier = modifier.fillMaxWidth().padding(14.dp).clickable { onClick(model) }) {
            RadioButton(
            selected = model.selected,
            modifier = modifier.testTag(model.id),
            onClick = { onClick(model) }
            )
            Text(text = model.itemName, modifier = modifier.padding(horizontal = 12.dp))
        }
    }
