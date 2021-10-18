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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Date
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
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
fun FormItem(form: QuestionnaireConfig, clickHandler: (form: QuestionnaireConfig) -> Unit) {
  Card(
    backgroundColor = colorResource(id = R.color.cornflower_blue),
    elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().clickable { clickHandler(form) }
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
      Text(
        text = form.title.uppercase(),
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
      }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp)
      ) {
        Spacer(Modifier.height(24.dp))
        Card(
          elevation = 3.dp,
          backgroundColor = colorResource(id = R.color.white),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {

            // fetch forms
            val forms = dataProvider.getAllForms().observeAsState()
            forms.value?.let { allForms ->
              allForms.forEachIndexed { index, it ->
                FormItem(it) { dataProvider.onFormItemClickListener().invoke(it) }

                if (index < allForms.size.minus(1)) {
                  Spacer(Modifier.height(16.dp))
                }
              }
            }
          }
        }

        // responses section
        Spacer(Modifier.height(24.dp))
        Text(
          text = "RESPONSES",
          color = colorResource(id = R.color.grayText),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Card(
          elevation = 4.dp,
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
          Column() {
            // fetch responses
            val results = dataProvider.getAllResults().observeAsState()

            val totalResultsCount = results.value?.count() ?: 0
            results.value?.forEachIndexed { index, item ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                  Modifier.fillMaxWidth().padding(12.dp).clickable {
                    dataProvider.onTestResultItemClickListener().invoke(item)
                  }
              ) {
                Text(
                  text = (item.meta?.tagFirstRep?.display
                      ?: "") + " (${item.authored?.asDdMmmYyyy() ?: ""}) ",
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

              if (index < totalResultsCount) {
                Divider(color = colorResource(id = R.color.white_smoke))
              }
            }
          }
        }

        // for bottom padding
        Spacer(Modifier.height(24.dp))
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
                identifier = listOf(Identifier().apply { value = "12345" })
              }
            )
        }
      )
    }

    override fun getAllForms(): LiveData<List<QuestionnaireConfig>> {
      return MutableLiveData(
        listOf(QuestionnaireConfig("sample-test-result", "+ Sample Test Result", "12345"))
      )
    }

    override fun getAllResults(): LiveData<List<QuestionnaireResponse>> {
      return MutableLiveData(
        listOf(
          QuestionnaireResponse().apply {
            meta = Meta().apply { tag = listOf(Coding().apply { display = "Sample Test" }) }
            authored = Date()
          }
        )
      )
    }

    override fun onFormItemClickListener(): (item: QuestionnaireConfig) -> Unit {
      return {}
    }

    override fun onTestResultItemClickListener(): (item: QuestionnaireResponse) -> Unit {
      return {}
    }
  }
}
