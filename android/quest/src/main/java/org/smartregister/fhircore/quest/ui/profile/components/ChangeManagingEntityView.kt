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

package org.smartregister.fhircore.quest.ui.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity

const val TEST_TAG_SAVE = "saveTestTag"
const val TEST_TAG_CANCEL = "cancelTestTag"

@Composable
fun ChangeManagingEntityView(
  modifier: Modifier = Modifier,
  eligibleManagingEntities: List<EligibleManagingEntity> = emptyList(),
  onSaveClick: (EligibleManagingEntity) -> Unit,
  managingEntity: ManagingEntityConfig? = null,
  onDismiss: () -> Unit
) {
  var isEnabled by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
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
            text = managingEntity?.dialogTitle ?: "",
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp,
          )
          Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = null,
            tint = DefaultColor.copy(0.8f),
            modifier = modifier.clickable { onDismiss() }
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
                shape = RoundedCornerShape(8.dp)
              )
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_alert_triangle),
            contentDescription = null,
            modifier = modifier.padding(horizontal = 12.dp)
          )
          Text(
            text = managingEntity?.dialogWarningMessage ?: "",
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = modifier.padding(vertical = 12.dp)
          )
        }
        Text(
          text = managingEntity?.dialogContentMessage ?: "",
          modifier = modifier.padding(horizontal = 12.dp),
          textAlign = TextAlign.Start,
          fontWeight = FontWeight.Light,
          fontSize = 18.sp,
        )
      }
    },
    bottomBar = {
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
          onClick = { onDismiss() },
          modifier = modifier.fillMaxWidth().weight(1F).testTag(TEST_TAG_CANCEL)
        ) {
          Text(
            fontSize = 14.sp,
            color = colorResource(id = R.color.black),
            text = stringResource(id = R.string.cancel),
          )
        }
        TextButton(
          enabled = isEnabled,
          onClick = {
            onSaveClick(eligibleManagingEntities.first { it.selected })
            onDismiss()
          },
          modifier = modifier.fillMaxWidth().weight(1F).testTag(TEST_TAG_SAVE),
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
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxWidth().wrapContentHeight()
      ) {
        itemsIndexed(
          items = eligibleManagingEntities,
          itemContent = { _, item ->
            BottomListItem(modifier, item) { managingEntity ->
              eligibleManagingEntities.forEach { it.selected = false }
              managingEntity.selected = true
              isEnabled = true
            }
            Divider(color = DividerColor, thickness = 1.dp)
          }
        )
      }
    }
  }
}

@Composable
fun BottomListItem(
  modifier: Modifier = Modifier,
  managingEntity: EligibleManagingEntity,
  onClick: (EligibleManagingEntity) -> Unit
) {
  Row(
    modifier = modifier.fillMaxWidth().clickable { onClick(managingEntity) },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = managingEntity.selected, onClick = { onClick(managingEntity) })
    Text(text = managingEntity.memberInfo)
  }
}

@Preview(showBackground = true)
@Composable
fun ChangeManagingEntityViewPreview() {
  ChangeManagingEntityView(
    onSaveClick = {},
    eligibleManagingEntities =
      listOf(
        EligibleManagingEntity(
          groupId = "group-1",
          logicalId = "patient-1",
          memberInfo = "Jane Doe"
        ),
        EligibleManagingEntity(
          groupId = "group-1",
          logicalId = "patient-2",
          memberInfo = "James Doe"
        )
      ),
    onDismiss = {},
    managingEntity =
      ManagingEntityConfig(
        nameFhirPathExpression = "Patient.name",
        eligibilityCriteriaFhirPathExpression = "Patient.active",
        resourceType = ResourceType.Patient,
        dialogTitle = "Assign new family head",
        dialogWarningMessage = "Are you sure you want to abort this operation?",
        dialogContentMessage = "Select a new family head"
      )
  )
}
