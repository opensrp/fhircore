package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.R

@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier, onClickRetry: () -> Unit) {
  Row(
    modifier = modifier.padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = message,
      maxLines = 1,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.h6,
      color = MaterialTheme.colors.error
    )
    OutlinedButton(onClick = onClickRetry) { Text(text = stringResource(R.string.try_again)) }
  }
}

@Preview(showBackground = true)
@Composable
fun ErrorMessagePreview() {
  ErrorMessage(message = "An error occurred", onClickRetry = {})
}
