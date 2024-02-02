package org.smartregister.fhircore.quest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class DataActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ConfirmClearDataScreen()
      }
    }
  }
}

@Composable
fun ConfirmClearDataScreen() {
  var isConfirmationDialogVisible by remember { mutableStateOf(false) }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            "Warning: Clearing app data will result in the loss of all locally stored data.",
            color = Color.Red,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { isConfirmationDialogVisible = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
              Text("Clear App Data")
            }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp)) {
          Text("Back up and clear App Data")
        }

        if (isConfirmationDialogVisible) {
          ClearDataConfirmationDialog(
              onConfirm = {
                // Handle clearing app data
                // ...
                isConfirmationDialogVisible = false
              },
              onDismiss = { isConfirmationDialogVisible = false })
        }
      }
}

@Composable
fun ClearDataConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = { onDismiss.invoke() },
      title = { Text("Confirm App's Data ") },
      text = { Text("Are you sure you want to clear the app's data?") },
      confirmButton = { Button(onClick = { onConfirm.invoke() }) { Text("Proceed") } },
      dismissButton = { Button(onClick = { onDismiss.invoke() }) { Text("Cancel") } })
}

@Preview(showBackground = true)
@Composable
fun ClearDataScreenPreview() {
  ConfirmClearDataScreen()
}
