package org.smartregister.fhircore.quest.ui.patient.details

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.R


@Composable
fun QuestPatientTestResultScreen(dataProvider: QuestPatientDetailDataProvider) {

  Surface(color = colorResource(id = R.color.white_smoke)) {
    Column {

      TopAppBar(
        title = { Text(text = stringResource(id = R.string.back_to_clients)) },
        navigationIcon = {
          IconButton(onClick = {
            dataProvider.onBackPressListener().invoke()
          }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow")
          }
        }
      )

      // full name with gender and age
      val patient = dataProvider.getDemographics().observeAsState().value

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = colorResource(id = R.color.colorPrimary))
            .padding(12.dp)
      ) {
        Text(
          text = patient?.extractName() ?: "",
          color = colorResource(id = R.color.white),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${patient?.extractGender(LocalContext.current)} - ${patient?.extractAge() ?: ""}",
          color = colorResource(id = R.color.cornflower_blue),
          fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "TEST RESULTS",
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
          text = "No test results found",
          color = colorResource(id = R.color.grayText),
          fontSize = 15.sp,
        )

      }
    }
  }
}

@Preview
@Composable
fun PreviewQuestPatientTestResultScreen() {
  AppTheme { QuestPatientTestResultScreen(dummyQuestPatientDetailDataProvider()) }
}
