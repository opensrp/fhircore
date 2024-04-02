package org.smartregister.fhircore.quest.ui.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R

@Composable
fun BottomSheetContent(
    modifier: Modifier = Modifier,
    listImageProperties: List<ImageProperties>,
    navController: NavController,
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // Icon
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_check_circled),
            contentDescription = "Icon",
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Top)
        )

        // Spacer between Icon and Title
        Spacer(modifier = Modifier.width(16.dp))

        // Title and Right Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Title",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Three Rows
            Text(
                text = "Row 1",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Row 2",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Row 3",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 16.sp
            )
        }
    }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun BottomSheetExample() {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {

    }
}