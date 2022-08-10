package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType

@Serializable
data class CardViewProperties (
    override val viewType: ViewType,
    val content: List<ViewProperties> = emptyList(),
    val elevation:Int = 5,
    val cornerSize:Int = 6,
    val padding:Int = 16
) : ViewProperties()