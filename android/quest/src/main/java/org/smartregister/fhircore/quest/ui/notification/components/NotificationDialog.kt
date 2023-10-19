package org.smartregister.fhircore.quest.ui.notification.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.smartregister.fhircore.quest.R

@Composable
fun NotificationDialog(
    title: String,
    description: String,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissDialog,
        content = {
            Column(Modifier.background(Color.White)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(20.dp)
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, false)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        modifier = modifier
                            .padding(horizontal = 10.dp)
                            .clickable { onDismissDialog() },
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewNotificationDialog() {
    NotificationDialog(
        title = "Notification Title",
        description = "Notification Description",
        onDismissDialog = { /*TODO*/ })
}