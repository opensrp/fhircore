package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.view.BorderProperties
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

@Composable
fun BorderView(
    modifier: Modifier = Modifier,
    borderProperties: BorderProperties,
) {
    if (borderProperties.thickness != null) {
        Divider(
            color = DividerColor, thickness = borderProperties.thickness!!.dp
        )
    }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun BorderPreview() {
    BorderView(borderProperties = BorderProperties(thickness = 16f))
}