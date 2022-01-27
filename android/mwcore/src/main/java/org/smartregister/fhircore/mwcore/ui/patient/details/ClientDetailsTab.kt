package org.smartregister.fhircore.mwcore.ui.patient.details

import androidx.compose.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.ui.layout.Column
import com.google.android.material.tabs.TabItem
import kotlinx.coroutines.NonCancellable.children
import kotlinx.serialization.json.JsonNull.content
import org.intellij.lang.annotations.JdkConstants


sealed class TabItem(
    val index:Int,
    val icon:ImageVector,
    val title:String,
    val screenToLoad: @Composable ()-> Unit
){
    object Demographic : org.smartregister.fhircore.mwcore.ui.patient.details.TabItem(0, Icons.Filled.Feed, "Details", {
        DemographicsTab()
    })
    object Visit : org.smartregister.fhircore.mwcore.ui.patient.details.TabItem(2, Icons.Filled.DirectionsWalk, "Visit", {
        VisitTab()
    })
    object History : org.smartregister.fhircore.mwcore.ui.patient.details.TabItem(1, Icons.Filled.History, "Settings", {
        HistoryTab()
    })

}

@Composable
fun DemographicsTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "You are in Demographics Screen")
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

 private  val tabData = listOf(
     org.smartregister.fhircore.mwcore.ui.patient.details.TabItem.Demographic,
     org.smartregister.fhircore.mwcore.ui.patient.details.TabItem.Visit,
     org.smartregister.fhircore.mwcore.ui.patient.details.TabItem.History
 )