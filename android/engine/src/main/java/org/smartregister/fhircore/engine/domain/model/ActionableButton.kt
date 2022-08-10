package org.smartregister.fhircore.engine.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.InfoColor

data class ActionableButton (
    val action: String,
    val questionnaire: QuestionnaireConfig? = null,
    val backReference: Reference? = null,
    val contentColor: Color = InfoColor,
    val iconColor: Color = InfoColor,
    val iconStart: ImageVector = Icons.Filled.Add
)