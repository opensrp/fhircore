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

package org.smartregister.fhircore.quest.ui.main.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.quest.R

const val REQUIRED_PERMISSION_DIALOG = "requiredPermissionDialog"

@Composable
fun RequiredPermissionDialog(
    context: Context,
    permissions: String,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissDialog,
        title = {
            Text(
                text = stringResource(R.string.required_permission_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = { Text(text = stringResource(R.string.required_permission_message, permissions), fontSize = 16.sp) },
        buttons = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    modifier = modifier
                        .padding(horizontal = 10.dp)
                        .clickable { onDismissDialog() },
                )
                Text(
                    color = MaterialTheme.colors.primary,
                    text = stringResource(R.string.settings).uppercase(),
                    modifier =
                    modifier
                        .padding(horizontal = 10.dp)
                        .clickable {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                context.startActivity(this)
                            }

                            onDismissDialog()
                        },
                )
            }
        },
        modifier = Modifier.testTag(REQUIRED_PERMISSION_DIALOG),
    )
}