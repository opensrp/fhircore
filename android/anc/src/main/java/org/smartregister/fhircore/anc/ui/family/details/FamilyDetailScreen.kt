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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import java.text.SimpleDateFormat


@Composable
fun FamilyDetailScreen(dataProvider: FamilyDetailDataProvider) {

  Surface(
    color = colorResource(id = R.color.white_smoke)
  ) {
    Column(
      /*modifier = Modifier
        .background(color = colorResource(id = R.color.white_smoke))*/
    ) {

      // top bar
      TopAppBar(
        title = {
          Text(text = stringResource(id = R.string.all_families))
        },
        navigationIcon = {
          IconButton(onClick = {
            dataProvider.getAppBackClickListener().invoke()
          }) {
            Icon(
              Icons.Filled.ArrowBack,
              contentDescription = "Back arrow"
            )
          }
        }
      )

      // family name
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(color = colorResource(id = R.color.colorPrimary))
          .padding(12.dp)
      ) {

        val patient = dataProvider.getDemographics().observeAsState()
        Text(
          text = patient.value?.name?.first()?.family ?: "",
          color = colorResource(id = R.color.white),
          fontSize = 25.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = patient.value?.name?.first()?.given?.first()?.value ?: "",
          color = colorResource(id = R.color.white),
          fontSize = 25.sp
        )

      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(start = 12.dp, end = 12.dp)
          .verticalScroll(rememberScrollState()),
      ) {

        // spacer for padding
        Spacer(Modifier.height(12.dp))

        // members heading
        MemberHeading()

        // members list
        dataProvider.getFamilyMembers().observeAsState().value?.run {
          MembersList(this, dataProvider.getMemberItemClickListener(), dataProvider.getAddMemberItemClickListener())
        }

        // encounter heading and see all button
        EncounterHeader(dataProvider.getSeeAllEncounterClickListener())

        // encounter list
        dataProvider.getEncounters().observeAsState().value?.run {
          EncounterList(this, dataProvider.getEncounterItemClickListener())
        }

        // spacer for padding
        Spacer(Modifier.height(12.dp))
      }

    }
  }

}

@Composable
fun MemberHeading() {
  Text(
    text = stringResource(id = R.string.members).uppercase(),
    color = colorResource(id = R.color.status_gray),
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Start,
    fontSize = 16.sp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp)
  )
}

@Composable
fun MembersList(members: List<FamilyMemberItem>, memberItemClickListener: (item: FamilyMemberItem) -> Unit, addMemberItemClickListener: () -> Unit) {

  Card(
    elevation = 4.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(),
    ) {

      val totalMemberCount = members.count()
      members.forEachIndexed { index, item ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { memberItemClickListener(item) }
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
            contentDescription = "",
            colorFilter = ColorFilter.tint(colorResource(id = R.color.status_gray))
          )
        }

        if (index < totalMemberCount) {
          Box(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = colorResource(id = R.color.white_smoke)))
        }
      }
      
      TextButton(
        onClick = { addMemberItemClickListener() },
        modifier = Modifier
          .fillMaxWidth()
      ) {
        Text(
          text = stringResource(id = R.string.add_member).uppercase(),
          color = colorResource(id = R.color.colorPrimaryLight),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
      }

    }
  }
}

@Composable
fun EncounterHeader(seeAllEncounterClickListener: () -> Unit) {

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 18.dp)
  ) {

    // encounter heading
    Text(
      text = stringResource(id = R.string.encounters).uppercase(),
      color = colorResource(id = R.color.status_gray),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Start,
      fontSize = 16.sp
    )

    // button see all encounters
    TextButton(
      contentPadding = PaddingValues(0.dp),
      onClick = {
        seeAllEncounterClickListener()
      }
    ) {
      Text(
        text = stringResource(id = R.string.see_all).uppercase(),
        color = colorResource(id = R.color.colorPrimaryLight),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
      )

      Icon(
        painterResource(id = R.drawable.ic_forward_arrow),
        "",
        Modifier.padding(start = 4.dp)
      )
    }
  }
}

@Composable
fun EncounterList(members: List<Encounter>, encounterItemClickListener: (item: Encounter) -> Unit) {

  Card(
    elevation = 4.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(),
    ) {

      val totalMemberCount = members.count()
      members.forEachIndexed { index, item ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
              encounterItemClickListener(item)
            }
        ) {

          Row(
            verticalAlignment = Alignment.CenterVertically
          ){

            Icon(
              Icons.Filled.TaskAlt,
              null,
              Modifier
                .width(20.dp)
                .height(20.dp)
            )

            Text(
              text = item.class_?.display ?: "",
              color = colorResource(id = R.color.black),
              fontSize = 17.sp,
              textAlign = TextAlign.Start,
              modifier = Modifier.padding(start = 6.dp, end = 12.dp)
            )
          }

          Text(
            text = SimpleDateFormat.getDateInstance().format(item.period.start),
            color = colorResource(id = R.color.status_gray),
            fontSize = 17.sp,
            textAlign = TextAlign.Start
          )
        }

        if (index < totalMemberCount) {
          Box(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = colorResource(id = R.color.white_smoke)))
        }
      }

    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewFamilyDetailScreen() {
  AppTheme {
    FamilyDetailScreen(getDummyDataProvider())
  }
}

fun getDummyDataProvider() : FamilyDetailDataProvider {
  return object: FamilyDetailDataProvider{
    override fun getDemographics(): LiveData<Patient> {
      return MutableLiveData(Patient().apply {
        name = listOf(HumanName().apply {
          given = listOf(StringType("John"))
          family = "Doe"
        })
      })
    }

    override fun getFamilyMembers(): LiveData<List<FamilyMemberItem>> {
      return MutableLiveData(
        listOf(
          dummyFamilyMemberItem("Kevin"),
          dummyFamilyMemberItem("Julie"),
          dummyFamilyMemberItem("Salina")
        )
      )
    }

    override fun getEncounters(): LiveData<List<Encounter>> {
      return MutableLiveData(
        listOf(
          dummyEncounter("Encounter 1", "2020-05-22"),
          dummyEncounter("Encounter 2", "2020-11-15"),
          dummyEncounter("Encounter 3", "2021-02-08"),
          dummyEncounter("Encounter 4", "2021-07-18")
        )
      )
    }
  }
}

private fun dummyFamilyMemberItem(name: String): FamilyMemberItem {
  return FamilyMemberItem(name, "1", "18", "Male", false)
}

private fun dummyEncounter(text: String, periodStartDate: String) : Encounter {
  return Encounter().apply {
    class_ = Coding("", "", text)
    period = Period().apply {
      start = SimpleDateFormat("yyyy-MM-dd").parse(periodStartDate)
    }
  }
}