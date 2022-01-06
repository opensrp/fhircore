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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.patientDetailsViewConfigurationOf

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
fun Toolbar(questPatientDetailViewModel: QuestPatientDetailViewModel) {
  var showMenu by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      Text(text = stringResource(id = R.string.back_to_clients), Modifier.testTag(TOOLBAR_TITLE))
    },
    navigationIcon = {
      IconButton(
        onClick = { questPatientDetailViewModel.onBackPressed(true) },
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
            questPatientDetailViewModel.onMenuItemClickListener(R.string.edit_patient_info)
          }
        ) { Text(text = stringResource(id = R.string.edit_patient_info)) }
        DropdownMenuItem(
          onClick = {
            showMenu = false
            questPatientDetailViewModel.onMenuItemClickListener(R.string.test_results)
          }
        ) { Text(text = stringResource(id = R.string.test_results)) }
        DropdownMenuItem(
          onClick = {
            showMenu = false
            questPatientDetailViewModel.onMenuItemClickListener(R.string.run_cql)
          }
        ) { Text(text = stringResource(id = R.string.run_cql)) }
      }
    }
  )
}

@Composable
fun ResultItem(
  testResult: Pair<QuestionnaireResponse, Questionnaire>,
  questPatientDetailViewModel: QuestPatientDetailViewModel
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      Modifier.fillMaxWidth()
        .padding(12.dp)
        .clickable { questPatientDetailViewModel.onTestResultItemClickListener(testResult.first) }
        .testTag(RESULT_ITEM)
  ) {
    Text(
      text = (questPatientDetailViewModel.fetchResultItemLabel(testResult)
          ?: "") + " (${testResult.first.authored?.asDdMmmYyyy() ?: ""}) ",
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
fun FormItem(
  questionnaireConfig: QuestionnaireConfig,
  questPatientDetailViewModel: QuestPatientDetailViewModel
) {
  Card(
    backgroundColor = colorResource(id = R.color.cornflower_blue),
    modifier =
      Modifier.fillMaxWidth()
        .clickable { questPatientDetailViewModel.onFormItemClickListener(questionnaireConfig) }
        .testTag(FORM_ITEM)
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
      Text(
        text = questionnaireConfig.title.uppercase(),
        color = colorResource(id = R.color.colorPrimary),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun QuestPatientDetailScreen(questPatientDetailViewModel: QuestPatientDetailViewModel) {
  val viewConfiguration by questPatientDetailViewModel.patientDetailsViewConfiguration.observeAsState(patientDetailsViewConfigurationOf())
  val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)
  val forms by questPatientDetailViewModel.questionnaireConfigs.observeAsState(null)
  val testResults by questPatientDetailViewModel.testResults.observeAsState(null)

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      Toolbar(questPatientDetailViewModel)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
      ) {
        Text(
          text =
            "${patientItem?.name ?: ""}, ${patientItem?.gender ?: ""}, ${patientItem?.age ?: ""}",
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag(PATIENT_NAME)
        )
        // Adding Additional Data i.e, G6PD Status etc.
        patientItem?.additionalData?.forEach {
          Row {
            it.label?.let { label ->
              Text(
                text = label,
                color =
                  Color(
                    android.graphics.Color.parseColor(it.properties?.label?.color ?: "#000000")
                  ),
                fontSize = it.properties?.label?.textSize?.sp ?: 16.sp,
                modifier = Modifier.wrapContentWidth()
              )
            }

            it.valuePrefix?.let { prefix ->
              Text(
                text = viewConfiguration.valuePrefix,
                color =
                  Color(
                    android.graphics.Color.parseColor(it.properties?.value?.color ?: "#000000")
                  ),
                fontSize = it.properties?.value?.textSize?.sp ?: 16.sp,
                modifier = Modifier.wrapContentWidth()
              )
            }

            Text(
              text = it.value,
              color =
                Color(android.graphics.Color.parseColor(it.properties?.value?.color ?: "#000000")),
              fontSize = it.properties?.value?.textSize?.sp ?: 16.sp,
              modifier = Modifier.wrapContentWidth()
            )
          }
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
            forms?.let { allForms ->
              allForms.forEachIndexed { index, it ->
                FormItem(it, questPatientDetailViewModel)
                if (index < allForms.size.minus(1)) {
                  Spacer(Modifier.height(16.dp))
                }
              }
            }
              ?: Text(text = stringResource(id = R.string.loading_forms))
          }
        }

        Spacer(Modifier.height(24.dp))

        // Responses section
        Text(
          text = "${viewConfiguration.contentTitle} (${testResults?.size?.toString() ?: ""})",
          color = colorResource(id = R.color.grayText),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Card(
          elevation = 4.dp,
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(RESULT_CONTAINER_ITEM)
        ) {
          Column {
            testResults?.let {
              it.forEachIndexed { index, item ->
                ResultItem(item, questPatientDetailViewModel)
                if (index < it.size - 1) {
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
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}
