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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
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
            questPatientDetailViewModel.onMenuItemClickListener(true)
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
    modifier = Modifier.fillMaxWidth().clickable { clickHandler(form) }.testTag(FORM_ITEM)
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
fun QuestPatientDetailScreen(questPatientDetailViewModel: QuestPatientDetailViewModel) {
  val patient by questPatientDetailViewModel.patient.observeAsState(null)
  val forms by questPatientDetailViewModel.questionnaireConfigs.observeAsState(null)
  val testResults by questPatientDetailViewModel.testResults.observeAsState(null)

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      Toolbar(questPatientDetailViewModel)

      // full name with gender and age
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
      ) {
        Text(
          text =
            "${patient?.extractName() ?: ""}, ${patient?.extractGender(LocalContext.current)?.first() ?: ""}, ${patient?.extractAge() ?: ""}",
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag(PATIENT_NAME)
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
            forms?.let { allForms ->
              allForms.forEachIndexed { index, it ->
                FormItem(it) { questPatientDetailViewModel.onFormItemClickListener(it) }

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
            val totalResultsCount = testResults?.count() ?: 0
            testResults?.forEachIndexed { index, item ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                  Modifier.fillMaxWidth()
                    .padding(12.dp)
                    .clickable { questPatientDetailViewModel.onTestResultItemClickListener(item) }
                    .testTag(RESULT_ITEM)
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
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}
