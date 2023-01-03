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

package org.smartregister.fhircore.quest.ui.report.measure.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Pair
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.quest.R

const val DATE_RANGE_TITLE_TEST_TAG = "dateRangeTitleTestTag"
const val DATE_RANGE_SEPARATOR_TEST_TAG = "dateRangeSeparatorTestTag"
const val CALENDAR_ICON_TEST_TAG = "calendarIconTestTag"

@Composable
fun DateSelectionBox(
  modifier: Modifier = Modifier,
  startDate: String = "",
  endDate: String = "",
  showDateRangePicker: Boolean = true,
  dateRange: MutableState<Pair<Long, Long>>,
  onDateRangeSelected: (Pair<Long, Long>) -> Unit
) {
  val context = LocalContext.current

  Column(
    modifier = modifier.wrapContentWidth(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = stringResource(id = R.string.date_range),
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      modifier = modifier.wrapContentWidth().testTag(DATE_RANGE_TITLE_TEST_TAG)
    )
    Spacer(modifier = modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DateRangeItem(text = startDate)
      Text(
        "-",
        fontSize = 18.sp,
        modifier = modifier.padding(horizontal = 8.dp).testTag(DATE_RANGE_SEPARATOR_TEST_TAG)
      )
      DateRangeItem(text = endDate)
      if (showDateRangePicker) {
        Icon(
          Icons.Filled.CalendarToday,
          stringResource(R.string.date_range),
          modifier =
            modifier
              .clickable {
                showDateRangePicker(
                  context = context,
                  dateRange = dateRange,
                  onDateRangeSelected = onDateRangeSelected
                )
              }
              .padding(8.dp)
              .testTag(CALENDAR_ICON_TEST_TAG)
        )
      }
    }
  }
}

fun showDateRangePicker(
  context: Context,
  dateRange: MutableState<Pair<Long, Long>>,
  onDateRangeSelected: (Pair<Long, Long>) -> Unit
) {
  val constraintsBuilder =
    CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
  MaterialDatePicker.Builder.dateRangePicker()
    .apply {
      setCalendarConstraints(constraintsBuilder)
      setTitleText(context.getString(R.string.select_date))
      setSelection(dateRange.value)
    }
    .build()
    .run {
      addOnPositiveButtonClickListener { selectedDateRange ->
        onDateRangeSelected(selectedDateRange)
      }
      val fragmentManager = context.getActivity()!!.supportFragmentManager
      show(fragmentManager, "DATE_PICKER_DIALOG_TAG")
    }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DateRangeSelectedPreview() {
  val dateRange = remember {
    mutableStateOf(Pair(Calendar.getInstance().timeInMillis, Calendar.getInstance().timeInMillis))
  }
  DateSelectionBox(
    startDate = "Start date",
    endDate = "End date",
    onDateRangeSelected = {},
    dateRange = dateRange
  )
}
