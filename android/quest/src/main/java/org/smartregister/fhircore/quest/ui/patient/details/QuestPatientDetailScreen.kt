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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.logicalId
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.R

@Composable
fun Toolbar(dataProvider: QuestPatientDetailDataProvider) {
  var showMenu by remember { mutableStateOf(false) }

  TopAppBar(
    title = { Text(text = stringResource(id = R.string.back_to_clients)) },
    navigationIcon = {
      IconButton(onClick = { dataProvider.onBackPressListener().invoke() }) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow")
      }
    },
    actions = {
      IconButton(onClick = { showMenu = !showMenu }) {
        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
      }
      DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
          onClick = {
            showMenu = false
            dataProvider.onMenuItemClickListener().invoke("")
          }
        ) { Text(text = stringResource(id = R.string.test_results)) }
      }
    }
  )
}

@Composable
fun FormItem(formName: String, clickHandler: (formName: String) -> Unit) {
  Card(
    backgroundColor = colorResource(id = R.color.cornflower_blue),
    elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().clickable { clickHandler(formName) }
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
      Text(
        text = formName.uppercase(),
        color = colorResource(id = R.color.colorPrimary),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun QuestPatientDetailScreen(dataProvider: QuestPatientDetailDataProvider) {

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      Toolbar(dataProvider)

      // full name with gender and age
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
      ) {
        val patient = dataProvider.getDemographics().observeAsState().value

        Text(
          text =
            "${patient?.extractName() ?: ""}, ${patient?.extractGender(LocalContext.current)?.first() ?: ""}, ${patient?.extractAge() ?: ""}",
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${patient?.address?.first()?.city ?: ""} - ID: ${patient?.logicalId ?: ""}",
          color = colorResource(id = R.color.cornflower_blue),
          fontSize = 16.sp
        )
      }

      // forms
      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp)
      ) {
        Spacer(Modifier.height(24.dp))
        Text(
          text = "FORMS",
          color = colorResource(id = R.color.grayText),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Card(
          elevation = 3.dp,
          backgroundColor = colorResource(id = R.color.white),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            val forms =
              listOf(
                "Household Survey",
                "Bednet Distribution Form",
                "Malaria Diagnosis Form",
                "Medicine Treatment Form",
                "G6PD Test Result Form"
              )

            forms.forEachIndexed { index, it ->
              FormItem(it) {}

              if (index < forms.size.minus(1)) {
                Spacer(Modifier.height(16.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun PreviewQuestionPatientDetailScreen() {
  AppTheme { QuestPatientDetailScreen(dummyQuestPatientDetailDataProvider()) }
}

fun dummyQuestPatientDetailDataProvider(): QuestPatientDetailDataProvider {
  return object : QuestPatientDetailDataProvider {
    override fun getDemographics(): LiveData<Patient> {
      return MutableLiveData(
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                id = "5583145"
                family = "Does"
                given = listOf(StringType("John"))
                gender = Enumerations.AdministrativeGender.MALE
                birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01")
                address =
                  listOf(
                    Address().apply {
                      city = "Nairobi"
                      country = "Keynya"
                    }
                  )
              }
            )
        }
      )
    }
  }
}
