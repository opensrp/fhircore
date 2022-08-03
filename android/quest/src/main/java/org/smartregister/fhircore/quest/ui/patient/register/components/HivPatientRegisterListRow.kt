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

package org.smartregister.fhircore.quest.ui.patient.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.ui.theme.FemalePinkColor
import org.smartregister.fhircore.engine.ui.theme.MaleBlueColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HivPatientRegisterListRow(data: RegisterViewData, onItemClick: (String) -> Unit) {
  Card(
    onClick = { onItemClick(data.logicalId) },
    modifier = Modifier.padding(8.dp).fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(16.dp).height(IntrinsicSize.Min)
    ) {
      Box(
        modifier =
          Modifier.background(color = Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
      ) {
        Text(
          text = data.identifier,
          modifier = Modifier.width(48.dp),
          textAlign = TextAlign.Center,
          overflow = TextOverflow.Visible,
          style = MaterialTheme.typography.caption
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.weight(1f).padding(8.dp)
      ) {
        Text(
          text = data.title,
          style = MaterialTheme.typography.h6,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = data.subtitle!!,
          style = MaterialTheme.typography.caption,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box(
        modifier =
          Modifier.size(48.dp)
            .background(color = data.serviceButtonBackgroundColor, shape = CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (data.serviceTextIcon != null) {
          Image(
            painterResource(id = data.serviceTextIcon),
            contentDescription = "ART",
            Modifier.size(36.dp)
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewHivFemalePatientRegisterRowItem() {
  HivPatientRegisterListRow(
    data =
      RegisterViewData(
        logicalId = "eddb38b9-5363-4bcc-8cb9-dbbc2b4cddd9",
        identifier = "      ",
        title = "Sasha Pinheiro",
        subtitle = "22yr, Client Already On ART",
        serviceTextIcon = R.drawable.baseline_woman_24,
        serviceButtonBackgroundColor = FemalePinkColor,
        registerType = RegisterData.HivRegisterData::class,
      ),
    onItemClick = {}
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewHivMalePatientRegisterRowItem() {
  HivPatientRegisterListRow(
    data =
      RegisterViewData(
        logicalId = "1212299",
        identifier = "123456",
        title = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff Sr",
        serviceTextIcon = R.drawable.baseline_man_24,
        subtitle = "26yr, Client Already On ART",
        serviceButtonBackgroundColor = MaleBlueColor,
        registerType = RegisterData.HivRegisterData::class,
      ),
    onItemClick = {}
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewHivExposedInfantPatientRegisterRowItem() {
  HivPatientRegisterListRow(
    data =
      RegisterViewData(
        logicalId = "1212299",
        identifier = "909099",
        title = "Ali Coyote(Child)",
        serviceTextIcon = R.drawable.baseline_child_care_fill_48,
        subtitle = "6mnths, Exposed Infant",
        serviceButtonBackgroundColor = MaleBlueColor,
        registerType = RegisterData.HivRegisterData::class,
      ),
    onItemClick = {}
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewHivFemalePregnantPatientRegisterRowItem() {
  HivPatientRegisterListRow(
    data =
      RegisterViewData(
        logicalId = "345667782",
        identifier = "556637",
        title = "Maggie Coyote(Mother)",
        serviceTextIcon = R.drawable.baseline_pregnant_woman_24,
        subtitle = "27yrs, ART Client",
        serviceButtonBackgroundColor = FemalePinkColor,
        registerType = RegisterData.HivRegisterData::class,
      ),
    onItemClick = {}
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewHivFemaleBreastfeedingPatientRegisterRowItem() {
  HivPatientRegisterListRow(
    data =
      RegisterViewData(
        logicalId = "345667782",
        identifier = "556637",
        title = "Maggie Coyote(Mother Breastfeeding)",
        serviceTextIcon = R.drawable.ic_person_breastfeeding_solid,
        subtitle = "27yrs, ART Client",
        serviceButtonBackgroundColor = FemalePinkColor,
        registerType = RegisterData.HivRegisterData::class,
      ),
    onItemClick = {}
  )
}
