package org.smartregister.fhircore.mwcore.ui.patient.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.layout.Column
import com.google.android.material.tabs.TabItem
import kotlinx.coroutines.NonCancellable.children
import kotlinx.serialization.json.JsonNull.content
import org.intellij.lang.annotations.JdkConstants
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile


data class TabItem(
    val index:Int,
    val icon:ImageVector,
    val title:String,
    val screenToLoad: @Composable ()-> Unit
)


@Composable
fun DemographicsTab(questPatientDetailViewModel: QuestPatientDetailViewModel) {
    val patientItem by questPatientDetailViewModel.patientItem.observeAsState(null)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Row(Modifier.fillMaxWidth()){

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
            ){
                Column(modifier = Modifier.wrapContentHeight().padding(6.dp)) {
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
        ){
            Button(onClick = { /*TODO*/ }, modifier = Modifier.background(color = MaterialTheme.colors.primary, RoundedCornerShape(30.dp)),
                enabled = true) {
                Text(text = "Add Guardian", fontSize = 20.sp )
            }
        }

    }
}



@Composable
fun VisitTab(){
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
fun HistoryTab(){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "You are in History Screen")
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun Tabpreview(){
}