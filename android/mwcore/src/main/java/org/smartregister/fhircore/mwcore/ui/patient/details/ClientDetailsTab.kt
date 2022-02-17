package org.smartregister.fhircore.mwcore.ui.patient.details

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.register.RegisterViewModel
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification


data class TabItem(
    val index: Int,
    val icon: ImageVector,
    val title: String,
    val screenToLoad: @Composable () -> Unit
)


@Composable
fun DemographicsTab(questPatientDetailViewModel: QuestPatientDetailViewModel, configurationRegistry: ConfigurationRegistry) {
    val context = LocalContext.current

    val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {

            Card(
                modifier = Modifier
                    .padding(15.dp)
                    .height(200.dp),
                elevation = 6.dp,
                shape = MaterialTheme.shapes.medium.copy(
                    androidx.compose.foundation.shape.CornerSize(
                        16.dp
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(6.dp)
                ) {
                    Text(text = "ART # " + (patientItem?.identifier ?: ""))
                    Text(text = "Name: " + (patientItem?.name ?: ""))
                    Text(text = "Age: " + (patientItem?.age ?: ""))
                    Text(text = "Gender: " + (patientItem?.genderFull() ?: ""))
                    Text(text = "Location: " + (patientItem?.address ?: ""))

                }
            }

        }
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtendedFloatingActionButton(
                onClick = {

                    startActivity(
                        context,
                        Intent(context, QuestionnaireActivity::class.java)
                            .putExtras(
                                QuestionnaireActivity.intentArgs(
                                    clientIdentifier = patientItem?.id,
                                    formName = getRegistrationForm(configurationRegistry),
                                    QuestionnaireType.EDIT
                                )
                            ), null
                    )

//                patientItem?.let { editPatient(it.id) }

                },
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = "add guardian icon"
                    )
                },
                modifier = Modifier.height(60.dp),
                backgroundColor = MaterialTheme.colors.primary,
                text = { Text("ADD GUARDIAN", fontSize = 16.sp) })
        }

    }
}


fun getRegistrationForm(configurationRegistry: ConfigurationRegistry): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER_CLIENT
    )
        .registrationForm
}

fun editPatient(uniqueIdentifier: String, registerViewModel: RegisterViewModel, context: Context) {

    startActivity(
        context,
        Intent(context, QuestionnaireActivity::class.java)
            .putExtras(
                QuestionnaireActivity.intentArgs(
                    clientIdentifier = uniqueIdentifier,
                    formName = registerViewModel.registerViewConfiguration.value?.registrationForm!!
                )
            ), null
    )
}


@Composable
fun VisitTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(text = "You are in Visit Screen")
    }
}

@Composable
fun HistoryTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "You are in History Screen")
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun Tabpreview() {
}