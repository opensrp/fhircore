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

package org.smartregister.fhircore.quest.ui.patient.profile.tranfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.text.Typography.bullet
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.ui.appsetting.getMessageFromException
import org.smartregister.fhircore.engine.ui.login.LOGIN_ERROR_TEXT_TAG
import org.smartregister.fhircore.quest.R

@Composable
fun TransferOutScreen(
  viewModel: TransferOutViewModel = hiltViewModel(),
  onBackPress: () -> Boolean,
) {
  val state by viewModel.state.collectAsState()
  val uploadState by viewModel.updateState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uploadState) {
    if (uploadState is DataLoadState.Success) {
      onBackPress()
      onBackPress()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.transfer_out)) },
        navigationIcon = {
          IconButton(onClick = { onBackPress() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      when (state) {
        is DataLoadState.Success -> {
          val data = (state as DataLoadState.Success<TransferOutScreenState>).data
          TransferOutScreenContainer(data, uploadState) { viewModel.transferPatient(context) }
        }
        is DataLoadState.Error -> {
          Column(
            Modifier.fillMaxWidth().align(Alignment.Center),
          ) {
            Text(
              fontSize = 14.sp,
              color = MaterialTheme.colors.error,
              text =
                stringResource(
                  id = getMessageFromException((state as DataLoadState.Error).exception),
                ),
              modifier =
                Modifier.wrapContentWidth()
                  .padding(vertical = 10.dp)
                  .align(Alignment.CenterHorizontally)
                  .testTag(LOGIN_ERROR_TEXT_TAG),
            )
            Button(onClick = { viewModel.fetchPatientDetails() }) { Text(text = "Retry") }
          }
        }
        else -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.padding(bottom = 16.dp),
              strokeWidth = 1.6.dp,
            )
          }
        }
      }
    }
  }
}

@Composable
fun TransferOutScreenContainer(
  data: TransferOutScreenState,
  uploadState: DataLoadState<Boolean>,
  onClick: () -> Unit,
) {
  val paragraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 12.sp))
  val steps =
    listOf(
      "Click Transfer out button",
      "A screen for writing emails will appear for you to fill in the transfer out request",
      "Fill in the name of the facility the patient is transferring to in the email",
      "Fill in any additional information in the email",
      "Send the email",
    )
  Column(
    Modifier.fillMaxSize().padding(28.dp),
  ) {
    Text(
      text =
        buildAnnotatedString {
          append("Are you sure you want to transfer ")
          withStyle(
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold).toSpanStyle(),
          ) {
            append(data.fullName)
          }
          append(" to a new facility?")
        },
      style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text =
        buildAnnotatedString {
          appendLine("To transfer out a patient, carry out the following steps")
          steps.forEach {
            withStyle(style = paragraphStyle) {
              append(bullet)
              append("\t\t")
              append(it)
            }
          }
          appendLine()
          appendLine()
          withStyle(
            style =
              MaterialTheme.typography.subtitle2
                .copy(
                  color = MaterialTheme.colors.error,
                )
                .toSpanStyle(),
          ) {
            appendLine(
              "Note: Once the process starts the patient will not be accessible. The transfer out process is not immediate and might take up to 24 hours before the client is accessible at the target facility",
            )
          }
        },
    )
    Spacer(Modifier.height(10.dp))
    if (uploadState is DataLoadState.Error) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              color = MaterialTheme.colors.error,
              shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
      ) {
        Text(
          text = "Something went wrong while, transferring out the patient, please try again..",
          color = MaterialTheme.colors.onError,
        )
      }
      Spacer(Modifier.height(10.dp))
    }

    Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth(),
      enabled = uploadState !is DataLoadState.Loading,
    ) {
      if (uploadState is DataLoadState.Loading) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Preparing information....")
      } else {
        Text(text = "Transfer Out")
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun TransferOutScreenContainerPreview() {
  val data =
    TransferOutScreenState(
      "",
      "Maliko",
      "",
      "Maliko Samuel",
    )
  Scaffold { innerPadding ->
    Box(
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      TransferOutScreenContainer(data, DataLoadState.Loading) {}
    }
  }
}
