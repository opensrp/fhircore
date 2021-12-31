package org.smartregister.fhircore.quest.data.patient.model

import androidx.compose.runtime.Stable
import org.smartregister.fhircore.quest.configuration.view.DetailViewRow
import org.smartregister.fhircore.quest.configuration.view.Filter

@Stable
data class DetailsViewItem(
    var label: String = "",
    val rows: MutableList<DetailsViewItemRow> = mutableListOf()
)

data class DetailsViewItemRow(
    val cells: MutableList<DetailsViewItemCell> = mutableListOf()
)

data class DetailsViewItemCell(
    val value: Any,
    val filter: Filter
)