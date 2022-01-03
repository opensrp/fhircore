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

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.ui.core.Direction
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.DynamicColor
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.Property
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemCell
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItemRow
import org.smartregister.fhircore.quest.util.getColor
import timber.log.Timber

private fun String?.value() = this ?: ""

const val DETAILS_TOOLBAR_TITLE = "detailsToolbarTitle"
const val DETAILS_TOOLBAR_BACK_ARROW = "detailsToolbarBackArrow"
const val DETAILS_DATA_ROWS = "detailsDataRows"
const val DETAILS_DATA_ROW = "detailsDataRow"

@Composable
fun SimpleDetailsScreen(dataProvider: SimpleDetailsDataProvider) {
  val dataItem by dataProvider.detailsViewItem.observeAsState()

  Surface(color = colorResource(id = R.color.white)) {
    Column {
      TopAppBar(
        title = { Text(text = dataItem?.label.value(), Modifier.testTag(DETAILS_TOOLBAR_TITLE)) },
        navigationIcon = {
          IconButton(
            onClick = { dataProvider.onBackPressed(true) },
            Modifier.testTag(DETAILS_TOOLBAR_BACK_ARROW)
          ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
        }
      )

      Column(modifier = Modifier.padding(20.dp).testTag(DETAILS_DATA_ROWS)) {
        dataItem?.rows?.forEachIndexed { i, r ->
          kotlin
            .runCatching {
              Row(Modifier.padding(10.dp).testTag(DETAILS_DATA_ROW)) {
                r.cells.forEach { c ->
                  if (c.filter.properties?.labelDirection == Direction.UP) {
                    Column(modifier = Modifier.weight(1f).padding(5.dp)) { DetailsViewCell(c) }
                  } else Row(modifier = Modifier.weight(1f).padding(5.dp)) { DetailsViewCell(c) }
                }
              }

              // if no cells configured for in row add divider line
              if (r.cells.size == 0) {
                Divider(
                  color = colorResource(id = R.color.white_smoke),
                  modifier = Modifier.padding(20.dp)
                )
              }
            }
            .onFailure { Timber.e(it) }
        }
      }
    }
  }
}

@Composable
fun DetailsViewCell(cell: DetailsViewItemCell) {
  with(cell.filter) {
    this.label?.let {
      TextView(
        this.properties?.label,
        StringType(this.label),
        this.dynamicColors,
        this.properties?.valueFormatter
      )
    }

    TextView(
      this.properties?.value,
      cell.value,
      this.dynamicColors,
      this.properties?.valueFormatter
    )
  }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun TextView(
  property: Property?,
  value: Type?,
  colors: List<DynamicColor>?,
  valueFormatter: Map<String, String>?
) {
  val valueStr = value.valueToString()
  val dynamicColor = getColor(valueStr, colors)
  val color =
    when {
      dynamicColor?.isNotBlank() == true -> dynamicColor
      property?.color?.isNotBlank() == true -> property.color
      else -> "FF888888"
    }

  val formattedValue = valueFormatter?.get(valueStr) ?: valueStr
  val size = property?.textSize?.toFloat() ?: 16f
  Text(
    text = formattedValue,
    color = Color(color.toLong(radix = 16)),
    fontSize = TextUnit(size, TextUnitType.Sp),
  )
}

@ExcludeFromJacocoGeneratedReport
@Composable
@Preview
fun emptyView() {
  SimpleDetailsScreen(
    dataProvider =
      object : SimpleDetailsDataProvider {

        override val onBackPressClicked: LiveData<Boolean> = MutableLiveData(true)
        override fun onBackPressed(back: Boolean) {}
        override val detailsViewItem: LiveData<DetailsViewItem>
          get() = MutableLiveData(null)
      }
  )
}

@ExcludeFromJacocoGeneratedReport
@Composable
@Preview(showBackground = true)
fun simpleDetailsScreenView1() {
  val row1Props =
    Properties(
      labelDirection = Direction.UP,
      label = Property(color = "FF888888", textSize = 15),
      value = Property(textSize = 30)
    )

  val row3Props =
    Properties(
      labelDirection = Direction.UP,
      label = Property(color = "FF888888", textSize = 15),
      value = Property(textSize = 40)
    )

  val row4Props =
    Properties(
      labelDirection = Direction.UP,
      valueFormatter =
        mapOf(
          "Dynamic Value 1" to "Dynamic value 1 with a different text with value One",
          "Dynamic Value 2" to "Dynamic value 2 with a different text with value Two"
        )
    )

  SimpleDetailsScreen(
    object : SimpleDetailsDataProvider {
      override val onBackPressClicked: LiveData<Boolean> = MutableLiveData(true)
      override fun onBackPressed(back: Boolean) {}
      override val detailsViewItem: LiveData<DetailsViewItem>
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
                          StringType("Val 1"),
                          filterOf("key 1", "Sample Label 1", row1Props)
                        ),
                        DetailsViewItemCell(
                          StringType("Val 2"),
                          filterOf("key 2", "Sample Label Two", row1Props)
                        )
                      )
                  ),
                  DetailsViewItemRow(),
                  DetailsViewItemRow(
                    cells =
                      mutableListOf(
                        DetailsViewItemCell(
                          StringType("Value of Yellow"),
                          filterOf("key 1", "Label 1", row3Props)
                        )
                      )
                  ),
                  DetailsViewItemRow(
                    cells =
                      mutableListOf(
                        DetailsViewItemCell(
                          StringType("Dynamic Value 1"),
                          filterOf("key 1", "What is the value of Label", row4Props)
                        )
                      )
                  )
                )
            )
          )
    }
  )
}

@ExcludeFromJacocoGeneratedReport
@Composable
@Preview(showBackground = true)
fun simpleDetailsScreenView2() {
  val row1Props =
    Properties(label = Property(color = "FF8888FF", textSize = 16), value = Property(textSize = 16))

  val row3Props =
    Properties(
      labelDirection = Direction.UP,
      label = Property(color = "FF08F00F", textSize = 15),
      value = Property(textSize = 40)
    )

  val row4Props =
    Properties(
      labelDirection = Direction.UP,
      valueFormatter =
        mapOf(
          "Dynamic Value 1" to "Dynamic value 1 with a different text with value One",
          "Dynamic Value 2" to "Dynamic value 2 with a different text with value Two"
        )
    )

  SimpleDetailsScreen(
    object : SimpleDetailsDataProvider {
      override val onBackPressClicked: LiveData<Boolean> = MutableLiveData(true)
      override fun onBackPressed(back: Boolean) {}
      override val detailsViewItem: LiveData<DetailsViewItem>
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
                          StringType("Val 1"),
                          filterOf("key 1", "Sample Label 1", row1Props)
                        ),
                        DetailsViewItemCell(
                          StringType("Val 2"),
                          filterOf("key 2", "Sample Label Two", row1Props)
                        )
                      )
                  ),
                  DetailsViewItemRow(),
                  DetailsViewItemRow(
                    cells =
                      mutableListOf(
                        DetailsViewItemCell(
                          StringType("Value of Magenta"),
                          filterOf("key 1", "Label 1", row3Props)
                        )
                      )
                  ),
                  DetailsViewItemRow(
                    cells =
                      mutableListOf(
                        DetailsViewItemCell(
                          StringType("Dynamic Value 2"),
                          filterOf("key 1", "What is the value of Label", row4Props)
                        )
                      )
                  )
                )
            )
          )
    }
  )
}

fun filterOf(key: String, label: String, properties: Properties): Filter {
  return Filter(
    resourceType = Enumerations.ResourceType.ENCOUNTER,
    key = key,
    label = label,
    properties = properties,
    dynamicColors =
      listOf(
        DynamicColor("Value of Magenta", "FFFF0FF0"),
        DynamicColor("Value of Yellow", "FFFFFF00"),
      ),
    valueType = Enumerations.DataType.CODING,
    valueCoding = Code("sys", "cod", "disp")
  )
}
