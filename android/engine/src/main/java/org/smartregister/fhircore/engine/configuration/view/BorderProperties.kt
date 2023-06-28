package org.smartregister.fhircore.engine.configuration.view

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class BorderProperties(
    override val viewType: ViewType = ViewType.SPACER,
    override val weight: Float = 0f,
    override val backgroundColor: String? = "#FFFFFF",
    override val padding: Int = 0,
    override val borderRadius: Int = 2,
    override val alignment: ViewAlignment = ViewAlignment.NONE,
    override val fillMaxWidth: Boolean = false,
    override val fillMaxHeight: Boolean = false,
    override val clickable: String = "false",
    override val visible: String = "true",
    val thickness: Float? = null,
): ViewProperties(){
    override fun interpolate(computedValuesMap: Map<String, Any>): BorderProperties {
        return this.copy(
            backgroundColor = backgroundColor?.interpolate(computedValuesMap),
            visible = visible.interpolate(computedValuesMap)
        )
    }
}