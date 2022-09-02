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

package org.smartregister.fhircore.quest.ui.patient.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileRowItem
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileViewSection

@Composable
fun ProfileCard(
  title: String,
  profileViewSection: PatientProfileViewSection,
  onActionClick: ((PatientProfileViewSection) -> Unit),
  showSeeAll: Boolean = true,
  modifier: Modifier = Modifier,
  body: (@Composable() () -> Unit)
) {
  ProfileCard(
    title = { Text(text = title.uppercase(Locale.getDefault())) },
    profileViewSection = profileViewSection,
    onActionClick = onActionClick,
    showSeeAll,
    modifier,
    body
  )
}

@Composable
fun ProfileCard(
  title: @Composable RowScope.() -> Unit,
  profileViewSection: PatientProfileViewSection,
  onActionClick: (PatientProfileViewSection) -> Unit,
  showSeeAll: Boolean = true,
  modifier: Modifier = Modifier,
  body: (@Composable() () -> Unit)
) {
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
      title()

      if (showSeeAll) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.clickable { onActionClick(profileViewSection) }
        ) {
          TextButton(onClick = { onActionClick(profileViewSection) }) {
            Text(
              text = stringResource(R.string.see_all).uppercase(Locale.getDefault()),
              color = InfoColor
            )
          }
          Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = InfoColor
          )
        }
      }
    }

    Card(
      elevation = 5.dp,
      modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).padding(16.dp)
    ) { Column { body() } }
  }
}

@Composable
@Preview(showBackground = true)
private fun PatientProfileSectionPreview() {
  ProfileCard(
    title = "TASKS",
    onActionClick = {},
    profileViewSection = PatientProfileViewSection.TASKS
  ) {
    Column {
      ProfileActionableItem(
        PatientProfileRowItem(
          id = "1",
          title = "ANC",
          titleIcon = R.drawable.ic_pregnant,
          subtitle = "due date",
          profileViewSection = PatientProfileViewSection.TASKS,
          actionButtonColor = InfoColor,
          actionButtonText = "ANC visit"
        ),
        onActionClick = { _, _ -> }
      )
      Divider()
      ProfileActionableItem(
        PatientProfileRowItem(
          id = "2",
          title = "Sick",
          titleIcon = R.drawable.ic_pregnant,
          subtitle = "due date",
          profileViewSection = PatientProfileViewSection.TASKS,
          actionButtonColor = OverdueColor,
          actionButtonText = "Malaria medicine"
        ),
        onActionClick = { _, _ -> }
      )
    }
  }
}
