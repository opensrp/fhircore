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

package org.smartregister.fhircore.quest.ui.tracing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor

@Composable
fun InfoBoxItem(title: String, value: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.padding(4.dp)) {
    Text(text = title, modifier.padding(bottom = 4.dp), color = StatusTextColor, fontSize = 18.sp)
    Text(text = value, fontSize = 18.sp)
  }
}

@Composable
fun OutlineCard(modifier: Modifier = Modifier, content: (@Composable() () -> Unit)) {
  Card(
    elevation = 0.dp,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(width = 2.dp, color = StatusTextColor),
    modifier = modifier.fillMaxWidth(),
    content = content,
  )
}
