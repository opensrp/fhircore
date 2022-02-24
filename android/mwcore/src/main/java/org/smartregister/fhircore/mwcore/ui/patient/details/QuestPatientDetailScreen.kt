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

package org.smartregister.fhircore.mwcore.ui.patient.details

import android.content.Intent
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.End
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asDdMmmYyyy
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.configuration.view.patientDetailsViewConfigurationOf
import org.smartregister.fhircore.mwcore.data.patient.model.QuestResultItem
import org.smartregister.fhircore.mwcore.util.RegisterType.CLIENT_ID
import org.smartregister.fhircore.mwcore.util.RegisterType.EXPOSED_INFANT_ID


const val TOOLBAR_TITLE = "toolbarTitle"
const val TOOLBAR_BACK_ARROW = "toolbarBackArrow"
const val TOOLBAR_MENU_BUTTON = "toolbarMenuButtonTag"
const val TOOLBAR_MENU = "toolbarMenuTag"
const val PATIENT_NAME = "patientNameTag"
const val FORM_ITEM = "formItemTag"
const val RESULT_ITEM = "resultItemTag"
const val FORM_CONTAINER_ITEM = "formItemContainerTag"
const val RESULT_CONTAINER_ITEM = "resultItemContainerTag"

const val ADD_GUARDIAN = "Add Guardian"
const val ENTER_VIRAL_LOAD_RESULTS = "Enter Viral Load Results"
const val ENTER_DBS_RESULTS = "Enter DBS Results"

/*@Composable
fun Toolbar(questPatientDetailViewModel: QuestPatientDetailViewModel) {
  var showMenu by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      Text(text = stringResource(id = R.string.client_details), Modifier.testTag(TOOLBAR_TITLE))
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
      }
    }


  )
}*/

@Composable
fun ResultItem(
  testResult: QuestResultItem,
  questPatientDetailViewModel: QuestPatientDetailViewModel
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
    Modifier
      .fillMaxWidth()
      .padding(12.dp)
      .clickable { questPatientDetailViewModel.onTestResultItemClickListener(testResult) }
      .testTag(RESULT_ITEM)
  ) {
    Column(verticalArrangement = Arrangement.Center) {
      testResult.data.forEach { dataList ->
        Row(modifier = Modifier.padding(end = 12.dp)) {
          dataList.forEach { item ->
            item.label?.let {
              Text(
                text = item.label,
                color =
                Color(
                  android.graphics.Color.parseColor(item.properties?.label?.color ?: "#000000")
                ),
                fontSize = item.properties?.label?.textSize?.sp ?: 17.sp,
                fontWeight =
                FontWeight(item.properties?.label?.fontWeight?.weight ?: FontWeight.Normal.weight)
              )
            }

            Text(
              text = (item.valuePrefix ?: "") + item.value,
              color =
              Color(
                android.graphics.Color.parseColor(item.properties?.value?.color ?: "#000000")
              ),
              fontSize = item.properties?.value?.textSize?.sp ?: 17.sp,
              textAlign = TextAlign.Start,
              fontWeight =
              FontWeight(item.properties?.value?.fontWeight?.weight ?: FontWeight.Normal.weight)
            )
          }
        }
      }
    }

    Image(
      painter = painterResource(id = R.drawable.ic_forward_arrow),
      contentDescription = "",
      colorFilter = ColorFilter.tint(colorResource(id = R.color.status_gray))
    )
  }
}

//chisomoStart





@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabWithPager(questPatientDetailViewModel: QuestPatientDetailViewModel, configurationRegistry: ConfigurationRegistry, patientType: String) {
  val tabData = listOf(
    TabItem(0, Icons.Filled.Feed, "Details") {
      DemographicsTab(questPatientDetailViewModel, configurationRegistry, patientType)
    },
    TabItem(2, Icons.Filled.LocalHospital, "Visit") {
      VisitTab()
    },
    TabItem(1, Icons.Filled.History, "History") {
      HistoryTab()
    }
  )
  val pagerState = rememberPagerState(
    pageCount = tabData.size,
    infiniteLoop = true,
    initialPage = 0
  )
  val tabIndex = pagerState.currentPage
  val coroutineScope = rememberCoroutineScope()

  Column {
    TabRow(
      selectedTabIndex = tabIndex,

    ) {
      tabData.forEachIndexed { index, tabItem:TabItem ->
        Tab(selected = tabIndex == index, onClick = {
          //onPageSelected(tabItem)
          coroutineScope.launch {
            pagerState.animateScrollToPage(index)
          }
        },text = {
          Text(text = tabItem.title)
        }, icon = {
          Icon(tabItem.icon, "")
        })
      }
    }

    HorizontalPager(state = pagerState) {
      page: Int ->
      tabData[page].screenToLoad()
    }
  }
}





//chisomoEnd


/*@Composable
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
}*/


@Composable
fun QuestPatientDetailScreen(questPatientDetailViewModel: QuestPatientDetailViewModel, configurationRegistry: ConfigurationRegistry, patientType: String) {
  val viewConfiguration by questPatientDetailViewModel.patientDetailsViewConfiguration
    .observeAsState(patientDetailsViewConfigurationOf())
  val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)
  val forms by questPatientDetailViewModel.questionnaireConfigs.observeAsState(null)
  val testResults by questPatientDetailViewModel.testResults.observeAsState(null)
  val context = LocalContext.current
 // val PatientType: String? = QuestPatientDetailActivity().patientType
  Surface(color = colorResource(id = R.color.white_smoke)) {

  }
    Column {
      Row(
        modifier =
        Modifier
          .fillMaxWidth()
          .background(color = colorResource(id = R.color.colorPrimary))
          .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // back button
        IconButton(
          onClick = { questPatientDetailViewModel.onBackPressed(true) },
          Modifier.testTag(TOOLBAR_BACK_ARROW),
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "",
            colorFilter = ColorFilter.tint(colorResource(id = R.color.white))
          )
        }

        // toolbar title
        Text(
          text =
          patientItem?.name ?: "" /*, ${patientItem?.gender ?: ""}, ${patientItem?.age ?: ""}"*/,
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .testTag(PATIENT_NAME)
            .align(Alignment.CenterVertically)
        )

        // toolbar menu items
        val items: MutableList<String> = ArrayList()
        items.add(ADD_GUARDIAN)

        if (patientType == CLIENT_ID) {
          items.add(ENTER_VIRAL_LOAD_RESULTS)
        } else {
          items.add(ENTER_DBS_RESULTS)
        }

        Column() { //val items = listOf( "Test Results", "Edit Details", "Enter Lab Results")

        var showMenu by remember { mutableStateOf(false) }
        IconButton(
          onClick = { showMenu = !showMenu },
          modifier = Modifier
        ) { Image(
          painter = painterResource(id = R.drawable.ic_more_vert),
          contentDescription = "MenuOption Icon",
          colorFilter = ColorFilter.tint(colorResource(id = R.color.white))
        )}
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          Modifier.testTag(TOOLBAR_MENU)
        ) {

          items.forEachIndexed { _, s ->
            DropdownMenuItem(onClick = {
              showMenu = false

              val intent = Intent(context, QuestionnaireActivity::class.java)
              when (s) {
                  ADD_GUARDIAN -> {
                    intent.putExtras(QuestionnaireActivity.intentArgs(
                      clientIdentifier = null,
                      formName = "manage-guardian",
                      QuestionnaireType.DEFAULT
                    ))
                  }
                  ENTER_VIRAL_LOAD_RESULTS -> {
                    intent.putExtras(QuestionnaireActivity.intentArgs(
                      clientIdentifier = patientItem?.id,
                      formName = "client-viral-load-results",
                      QuestionnaireType.DEFAULT
                    ))
                  }
                  ENTER_DBS_RESULTS -> {
                    intent.putExtras(QuestionnaireActivity.intentArgs(
                      clientIdentifier = patientItem?.id,
                      formName = "exposed-infant-hiv-test-results",
                      QuestionnaireType.DEFAULT
                    ))
                  }
                  else -> {
                    Toast.makeText(context, "Invalid item selected", LENGTH_LONG).show()
                    return@DropdownMenuItem
                  }
              }

              ContextCompat.startActivity(context, intent, null)
              //onClick ends here

            }) {
              Text(text = "" + s)
             }
          }





//          DropdownMenuItem(
//            onClick = {
//              showMenu = false
//              questPatientDetailViewModel.onMenuItemClickListener(true)
//            }
//          ) { Text(text = stringResource(id = R.string.test_results)) }
        }


        }

      }
      TabWithPager(questPatientDetailViewModel, configurationRegistry, patientType)

      /* Forms section
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

        Spacer(Modifier.height(24.dp)) */

        /* Responses section
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
        }*/
      }
    }


@Preview
@Composable
fun TabPreview(){}