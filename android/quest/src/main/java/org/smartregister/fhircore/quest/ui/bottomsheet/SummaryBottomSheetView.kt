package org.smartregister.fhircore.quest.ui.bottomsheet

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

@Composable
fun SummaryBottomSheetView(
    modifier: Modifier = Modifier,
    properties: List<ViewProperties>,
    resourceData: ResourceData,
    navController: NavController,
) {
    ViewRenderer(
        viewProperties = properties,
        resourceData =
        resourceData,
        navController = navController,
    )
}
