package org.smartregister.fhircore.mwcore.ui.patient.details

import android.content.Context
import android.content.Intent
import android.graphics.fonts.FontStyle
import android.widget.ScrollView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.smartregister.fhircore.mwcore.util.RegisterType.CLIENT_ID


data class TabItem(
    val index: Int,
    val icon: ImageVector,
    val title: String,
    val screenToLoad: @Composable () -> Unit
)


@Composable
fun DemographicsTab(questPatientDetailViewModel: QuestPatientDetailViewModel, configurationRegistry: ConfigurationRegistry, patientType: String) {
    val context = LocalContext.current

    val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Card(
                modifier = Modifier
                    .padding(15.dp)
                    .weight(1f),
                elevation = 6.dp,
                shape = MaterialTheme.shapes.medium.copy(
                    androidx.compose.foundation.shape.CornerSize(
                        16.dp
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Demographics", style = MaterialTheme.typography.h6)
                    Text(text = "ART number: " + (patientItem?.identifier ?: ""))
                    Text(text = "Name: " + (patientItem?.name ?: ""))
                    Text(text = "Age: " + (patientItem?.age ?: ""))
                    Text(text = "Gender: " + (patientItem?.genderFull() ?: ""))
                    Text(text = "District: Mangochi")
                    Text(text = "TA: Mlombwa")
                    Text(text = "Village: Thondwe")
                    Text(text = "Locator: Next to the Borehole near St James Primary school " + (patientItem?.address ?: ""))
                    Text(text = "Phone: 088525892")
                    Text(text = "Phone Owner: Mother")
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = "Guardian", style = MaterialTheme.typography.h6)

                    Text(text = "Name: Janet Dzimbiri")
                    Text(text = "Gender: Female")
                    Text(text = "Relationship to client: Aunt")
                    Text(text = "Location: Next to St James Primary school")
                    Text(text = "Phone: 0994476384")
                    Text(text = "Phone owner: Husband")

                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = "Clinic Details", style = MaterialTheme.typography.h6)

                    Text(text = "Weight at initiation: 73kg")
                    Text(text = "Height at initiation: 173cm")
                    Text(text = "Test Type: DBS")
                    Text(text = "Date of test: 06/05/2018")
                    Text(text = "WHO Stage: 2")
                    Text(text = "CD4 count: <200")
                    Text(text = "TB History: Currently on treatment")
                    Text(text = "Regimen: 4A")
                    Text(text = "ART start date: 06/05/2018")
                }
            }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtendedFloatingActionButton(
                onClick = { onEditDetailsClicked(context, patientItem?.id, configurationRegistry, patientType) },
                modifier = Modifier.height(60.dp),
                backgroundColor = MaterialTheme.colors.primary,
                text = { Text("EDIT DETAILS", fontSize = 16.sp) },
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "edit icon"
                    )
                })
        }
    }
}

fun onEditDetailsClicked(context: Context, patientID: String?, configurationRegistry: ConfigurationRegistry, patientType: String) {
    startActivity(
        context,
        Intent(context, QuestionnaireActivity::class.java)
            .putExtras(
                QuestionnaireActivity.intentArgs(
                    clientIdentifier = patientID,
                    formName = getRegistrationForm(configurationRegistry, patientType),
                    QuestionnaireType.EDIT
                )
            ), null
    )
}

fun getRegistrationForm(configurationRegistry: ConfigurationRegistry, patientType: String): String {
    if (patientType == CLIENT_ID) {
        return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
            configClassification = MwCoreConfigClassification.PATIENT_REGISTER_CLIENT
        )
            .registrationForm
    }

    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER_EXPOSED_INFANT
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