/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.usersetting

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.LoginDarkColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.usersetting.components.UserSettingInsightView

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun UserSettingInsightScreen(
    unsyncedResources: List<Pair<String, Int>>,
    onRefreshRequest : () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(org.smartregister.fhircore.engine.R.string.insights)) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                contentColor = Color.White,
                backgroundColor = MaterialTheme.colors.primary,
            )
        },
        backgroundColor = colorResource(id = org.smartregister.fhircore.engine.R.color.backgroundGray),
    ) {
        Box(
            Modifier
                .clip(RectangleShape)
                .fillMaxSize()
                .background(Color.White)
        ) {
            LazyColumn {
                item {
                    UserSettingInsightView(
                        unsyncedResources = unsyncedResources,
                        syncedResources = unsyncedResources
                    )
                }

                item {
                    Column {
                        InfoView(text = stringResource(id = R.string.user_info))
                    }
            }
                item {
                    Column(
                        Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                            .padding(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(0.dp)) {
                            OutlinedButton(
                                modifier = Modifier.width(300.dp),
                                onClick = onRefreshRequest,
                                border = BorderStroke(0.7.dp, MaterialTheme.colors.primarySurface),
                            ) {
                                Text(
                                    text = stringResource(R.string.refresh),
                                    modifier = Modifier.padding(6.dp),
                                    style = TextStyle(
                                        color = MaterialTheme.colors.primarySurface,
                                        fontSize = 14.sp
                                    ),
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}


@Composable
fun InfoView(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = LoginDarkColor,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colorResource(id = org.smartregister.fhircore.engine.R.color.white))
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Text(text = text, fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = modifier.width(20.dp))
        }
    }
    Divider(color = DividerColor)
}



@Preview
@Composable
fun UserSettingInsightScreenPreview() {

    UserSettingInsightScreen(unsyncedResources = listOf(Pair("",1))) {
        
    }
}