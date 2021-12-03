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

package org.smartregister.fhircore.anc.ui.family.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.extension.makeItReadable

@Composable
fun FamilyDetailScreen(familyDetailViewModel: FamilyDetailViewModel) {
  val hasFamilyCarePlan =
    !familyDetailViewModel.familyCarePlans.observeAsState().value.isNullOrEmpty()
  val patient = familyDetailViewModel.demographics.observeAsState()

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      TopAppBar(
        title = { Text(text = stringResource(id = R.string.all_families)) },
        navigationIcon = {
          IconButton(onClick = familyDetailViewModel::onAppBackClick) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow")
          }
        }
      )

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
      ) {
        val familyName = patient.value?.name?.firstOrNull()?.family ?: ""
        val firstName = patient.value?.name?.firstOrNull()?.given?.firstOrNull()?.value ?: ""
        Text(
          text = "$familyName $firstName",
          color = colorResource(id = R.color.white),
          fontSize = 25.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = patient.value?.address?.firstOrNull()?.city.toString(),
          color = colorResource(id = R.color.white),
          fontSize = 25.sp
        )
      }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(start = 12.dp, end = 12.dp)
            .verticalScroll(rememberScrollState()),
      ) {
        if (hasFamilyCarePlan) {
          Spacer(Modifier.height(12.dp))
          HouseHoldTaskHeading()
          Spacer(Modifier.height(12.dp))
          MonthlyVisitHeading()
        }

        Spacer(Modifier.height(12.dp))

        MemberHeading(familyDetailViewModel::onAddMemberItemClicked)
        familyDetailViewModel.familyMembers.observeAsState().value?.run {
          MembersList(this, familyDetailViewModel::onMemberItemClick)
        }

        if (hasFamilyCarePlan) {
          UpcomingServiceHeader(familyDetailViewModel::onSeeUpcomingServicesListener)
          EncounterHeader(familyDetailViewModel::onSeeAllEncountersListener)
          familyDetailViewModel.encounters.observeAsState().value?.run {
            EncounterList(this, familyDetailViewModel::onEncounterItemClicked)
          }
          Spacer(Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
fun MemberHeading(onAddMemberItemClicked: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
  ) {
    Text(
      text = stringResource(id = R.string.members).uppercase(),
      color = colorResource(id = R.color.status_gray),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Start,
      fontSize = 16.sp,
    )
    TextButton(contentPadding = PaddingValues(0.dp), onClick = { onAddMemberItemClicked() }) {
      Text(
        text = stringResource(id = R.string.add).uppercase() + "+",
        color = colorResource(id = R.color.colorPrimaryLight),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
      )
    }
  }
}

@Composable
fun MembersList(
  members: List<FamilyMemberItem>,
  onMemberItemClick: (familyMemberItem: FamilyMemberItem) -> Unit
) {

  Card(
    elevation = 4.dp,
    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      val totalMemberCount = members.count()
      members.forEachIndexed { index, item ->
        Column(
          modifier = Modifier.fillMaxWidth().clickable { onMemberItemClick(item) },
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
          ) {
            Text(
              text = item.name,
              color = colorResource(id = R.color.black),
              fontSize = 17.sp,
              textAlign = TextAlign.Start,
              modifier = Modifier.padding(end = 12.dp)
            )

            Image(
              painter = painterResource(id = R.drawable.ic_forward_arrow),
              contentDescription = stringResource(id = R.string.forward_arrow),
              colorFilter = ColorFilter.tint(colorResource(id = R.color.status_gray))
            )
          }
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (item.houseHoldHead) {
              Text(
                text = stringResource(id = R.string.head_of_household),
                color = colorResource(id = R.color.status_gray),
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 12.dp)
              )
            }
          }
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (item.pregnant) {
              Image(
                painter = painterResource(R.drawable.ic_pregnant),
                contentDescription = stringResource(id = R.string.pregnant_woman),
                modifier = Modifier.padding(start = 12.dp)
              )
              Text(
                text = stringResource(id = R.string.anc_visit_due),
                color = colorResource(id = R.color.colorPrimaryLight),
                fontSize = 14.sp,
                textAlign = TextAlign.Start
              )
            }
          }
        }

        if (index < totalMemberCount) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(1.dp)
                .background(color = colorResource(id = R.color.white_smoke))
          )
        }
      }
    }
  }
}

@Composable
fun EncounterHeader(onSeeAllEncountersListener: () -> Unit) {

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
  ) {
    Text(
      text = stringResource(id = R.string.encounters).uppercase(),
      color = colorResource(id = R.color.status_gray),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Start,
      fontSize = 16.sp
    )
    TextButton(contentPadding = PaddingValues(0.dp), onClick = { onSeeAllEncountersListener() }) {
      Text(
        text = stringResource(id = R.string.see_all).uppercase(),
        color = colorResource(id = R.color.colorPrimaryLight),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
      )

      Icon(painterResource(id = R.drawable.ic_forward_arrow), "", Modifier.padding(start = 4.dp))
    }
  }
}

@Composable
fun EncounterList(members: List<Encounter>, onEncounterItemClicked: (item: Encounter) -> Unit) {

  Card(
    elevation = 4.dp,
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      val totalMemberCount = members.count()
      members.forEachIndexed { index, item ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier =
            Modifier.fillMaxWidth().padding(12.dp).clickable { onEncounterItemClicked(item) }
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TaskAlt, null, Modifier.width(20.dp).height(20.dp))

            Text(
              text = item.class_?.display ?: "",
              color = colorResource(id = R.color.black),
              fontSize = 17.sp,
              textAlign = TextAlign.Start,
              modifier = Modifier.padding(start = 6.dp, end = 12.dp)
            )
          }

          Text(
            text = item.period?.start.makeItReadable(),
            color = colorResource(id = R.color.status_gray),
            fontSize = 17.sp,
            textAlign = TextAlign.Start
          )
        }

        if (index < totalMemberCount) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(1.dp)
                .background(color = colorResource(id = R.color.white_smoke))
          )
        }
      }
    }
  }
}

@Composable
fun HouseHoldTaskHeading() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
  ) {
    Text(
      text = stringResource(id = R.string.household_tasks).uppercase(),
      color = colorResource(id = R.color.status_gray),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Start,
      fontSize = 16.sp,
    )
  }
}

@Composable
fun MonthlyVisitHeading() {
  Card(elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
      Text(
        text = "+" + stringResource(id = R.string.monthly_visit).uppercase(),
        color = colorResource(id = R.color.colorPrimaryLight),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun UpcomingServiceHeader(onSeeUpcomingServicesListener: () -> Unit) {

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
  ) {
    Text(
      text = stringResource(id = R.string.upcoming_services).uppercase(),
      color = colorResource(id = R.color.status_gray),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Start,
      fontSize = 16.sp
    )
    TextButton(
      contentPadding = PaddingValues(0.dp),
      onClick = { onSeeUpcomingServicesListener() }
    ) {
      Text(
        text = stringResource(id = R.string.see_all).uppercase(),
        color = colorResource(id = R.color.colorPrimaryLight),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
      )
      Icon(painterResource(id = R.drawable.ic_forward_arrow), "", Modifier.padding(start = 4.dp))
    }
  }
}
