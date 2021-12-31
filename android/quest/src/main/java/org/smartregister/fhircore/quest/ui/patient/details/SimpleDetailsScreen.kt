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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Colors
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import org.hl7.fhir.r4.model.Enumerations
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemCell
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemRow

private fun String?.value() = this ?: ""

@ExperimentalUnitApi
@Composable
fun SimpleDetailsScreen(dataProvider: SimpleDetailsDataProvider) {
  val dataItem by dataProvider.detailsViewItem.observeAsState()

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {
      TopAppBar(
        title = { Text(text = dataItem?.label.value(), Modifier.testTag(TOOLBAR_TITLE)) },
        navigationIcon = {
          IconButton(
            onClick = { dataProvider.onBackPressed(true) },
            Modifier.testTag(TOOLBAR_BACK_ARROW)
          ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
        }
      )

      Column {
        dataItem?.rows?.forEach { r ->
          Row {
            r.cells.forEach { c ->
              Column (modifier = Modifier
                .weight(1f)
                .padding(5.dp)){
                c.filter.valuePrefix?.let {
                  Text(
                    text = it,
                    color = Color(c.filter.color.toLong(radix = 16)),
                    fontSize = TextUnit(15f, TextUnitType.Sp),
                  )
                }
                Text(
                  text = c.value.toString(),
                  color = Color(c.filter.color.toLong(radix = 16)),
                  fontSize = TextUnit(15f, TextUnitType.Sp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@ExperimentalUnitApi
@ExcludeFromJacocoGeneratedReport
@Composable
@Preview
fun simpleDetailsScreenView() {
  SimpleDetailsScreen(
    object : SimpleDetailsDataProvider {
      override val detailsViewItem: MutableLiveData<DetailsViewItem>
        get() =
          MutableLiveData(
            DetailsViewItem(
              label = "My Sample Label",
              rows =
                mutableListOf(
                  DetailsViewItemRow(
                    cells =
                      mutableListOf(
                        DetailsViewItemCell(
                          "My test value 1",
                          filterOf("key 1", "Label 1")
                        ),
                        DetailsViewItemCell(
                          "My test value 2",
                          filterOf("key 2", "Label 2")
                        )
                      )
                  )
                )
            )
          )
    }
  )
}

private fun filterOf(key: String, label: String, size: Int): Filter {
  return Filter(
    resourceType = Enumerations.ResourceType.ENCOUNTER,
    color = "FF888888",
    key = key,
    valuePrefix = label,
    //TODO
    valueString = ""+size,
    valueType = Enumerations.DataType.CODING,
    valueCoding = Code("sys", "cod", "disp")
  )
}