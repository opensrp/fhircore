package org.smartregister.fhircore.quest.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer

@Composable
fun SummaryBottomSheetView(
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
