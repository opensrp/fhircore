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

import android.content.Context
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
import androidx.compose.ui.platform.testTag
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
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.R

const val TOOLBAR_TITLE = "toolbarTitle"
const val TOOLBAR_BACK_ARROW = "toolbarBackArrow"
const val TOOLBAR_MENU_BUTTON = "toolbarMenuButtonTag"
const val TOOLBAR_MENU = "toolbarMenuTag"
const val PATIENT_NAME = "patientNameTag"
const val FORM_ITEM = "formItemTag"
const val RESULT_ITEM = "resultItemTag"
const val FORM_CONTAINER_ITEM = "formItemContainerTag"
const val RESULT_CONTAINER_ITEM = "resultItemContainerTag"

@Composable
fun Toolbar(dataProvider: QuestPatientDetailDataProvider) {
  var showMenu by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      Text(text = stringResource(id = R.string.back_to_clients), Modifier.testTag(TOOLBAR_TITLE))
    },
    navigationIcon = {
      IconButton(
        onClick = { dataProvider.onBackPressListener().invoke() },
        Modifier.testTag(TOOLBAR_BACK_ARROW)
      ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
    },
    actions = {
      IconButton(
        onClick = { showMenu = !showMenu },
        modifier = Modifier.testTag(TOOLBAR_MENU_BUTTON)
      ) { Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null) }
      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        Modifier.testTag(TOOLBAR_MENU)
      ) {
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
fun FormItem(form: QuestionnaireConfig, dataProvider: QuestPatientDetailDataProvider) {
  Card(
    backgroundColor = colorResource(id = R.color.cornflower_blue),
    modifier =
      Modifier.fillMaxWidth()
        .clickable { dataProvider.onFormItemClickListener().invoke(form) }
        .testTag(FORM_ITEM)
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
fun ResultItem(form: QuestionnaireResponse, dataProvider: QuestPatientDetailDataProvider) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      Modifier.fillMaxWidth()
        .padding(12.dp)
        .clickable { dataProvider.onTestResultItemClickListener().invoke(form) }
        .testTag(RESULT_ITEM)
  ) {
    Text(
      text = (form.meta?.tagFirstRep?.display ?: "") + " (${form.authored?.asDdMmmYyyy() ?: ""}) ",
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
        dataProvider.getDemographics().observeAsState().value.let {
          Text(
            text = "${extractPatientBioData(it, LocalContext.current)}",
            color = colorResource(id = R.color.white),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(PATIENT_NAME)
          )
        }
      }

      // Forms section
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
          modifier = Modifier.fillMaxWidth().testTag(FORM_CONTAINER_ITEM)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {

            // fetch forms
            val forms = remember { mutableStateOf(dataProvider.getAllForms()) }
            forms.value.value?.let { allForms ->
              allForms.forEachIndexed { index, it ->
                FormItem(it, dataProvider)
                if (index < allForms.size.minus(1)) {
                  Spacer(Modifier.height(16.dp))
                }
              }
            }
              ?: Text(text = stringResource(id = R.string.loading_forms))
          }
        }

        Spacer(Modifier.height(24.dp))

        val responses = dataProvider.getAllResults().observeAsState()
        // Responses section
        Text(
          text = "RESPONSES (${responses.value?.size?.toString() ?: ""})",
          color = colorResource(id = R.color.grayText),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Card(
          elevation = 4.dp,
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(RESULT_CONTAINER_ITEM)
        ) {
          Column {
            // fetch responses
            responses.value?.let {
              it.forEachIndexed { index, item ->
                ResultItem(item, dataProvider)

                if (index < it.size) {
                  Divider(color = colorResource(id = R.color.white_smoke))
                }
              }
            }
              ?: Text(
                text = stringResource(id = R.string.loading_responses),
                modifier = Modifier.padding(16.dp)
              )
          }
        }

        // for bottom padding
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

fun extractPatientBioData(patient: Patient?, context: Context): String {
  return patient?.let {
    "${it.extractName()}, ${it.extractGender(context)?.first() ?: ""}, ${it.extractAge()}"
  }
    ?: ""
}

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
fun PreviewQuestionPatientDetailScreen() {
  AppTheme { QuestPatientDetailScreen(dummyQuestPatientDetailDataProvider()) }
}

// Dummy data providers initialized below
fun dummyQuestPatientDetailDataProvider(): QuestPatientDetailDataProvider {
  return object : QuestPatientDetailDataProvider {
    override fun getDemographics(): LiveData<Patient> {
      return MutableLiveData(
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                id = "5583145"
                family = "Doe"
                given = listOf(StringType("John"))
                gender = Enumerations.AdministrativeGender.MALE
                birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01")
                address =
                  listOf(
                    Address().apply {
                      city = "Nairobi"
                      country = "Kenya"
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
        listOf(
          QuestionnaireConfig(
            appId = "quest",
            form = "sample-order-result",
            title = "Sample Order Result",
            identifier = "12345"
          ),
          QuestionnaireConfig(
            appId = "quest",
            form = "sample-test-result",
            title = "Sample Test Result",
            identifier = "67890"
          )
        )
      )
    }

    override fun getAllResults(): LiveData<List<QuestionnaireResponse>> {
      return MutableLiveData(
        listOf(
          QuestionnaireResponse().apply {
            meta = Meta().apply { tag = listOf(Coding().apply { display = "Sample Order" }) }
            authored = Date()
          },
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

    override fun onMenuItemClickListener(): (menuItem: String) -> Unit {
      return {}
    }
  }
}

fun dummyEmptyPatientDetailDataProvider(): QuestPatientDetailDataProvider {
  return object : QuestPatientDetailDataProvider {
    override fun getDemographics(): LiveData<Patient> {
      return MutableLiveData()
    }

    override fun getAllForms(): LiveData<List<QuestionnaireConfig>> {
      return MutableLiveData()
    }

    override fun getAllResults(): LiveData<List<QuestionnaireResponse>> {
      return MutableLiveData()
    }

    override fun onFormItemClickListener(): (item: QuestionnaireConfig) -> Unit {
      return {}
    }

    override fun onTestResultItemClickListener(): (item: QuestionnaireResponse) -> Unit {
      return {}
    }
  }
}
