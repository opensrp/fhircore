package org.smartregister.fhircore.engine.configuration

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.ButtonType
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class ButtonPropertiesTest : RobolectricTest() {
    @Test
    fun testInterpolateInButtonProperties() {
        val buttonProperties = ButtonProperties(
            viewType = ViewType.BUTTON,
            weight = 0f,
            backgroundColor = "@{backgroundColor}",
            padding = 0,
            borderRadius = 2,
            alignment = ViewAlignment.NONE,
            fillMaxWidth = true,
            fillMaxHeight = false,
            clickable = "false",
            visible = "true",
            enabled = "@{enabled}",
            text = "@{text}",
            status = "@{status}",
            smallSized = false,
            fontSize = 14.0f,
            actions = emptyList(),
            buttonType = ButtonType.MEDIUM,
        )
        val map = mutableMapOf<String, String>()
        map["status"] = "DUE"
        map["backgroundColor"] = "#FFA500"
        map["enabled"] = "true"
        map["text"] = "ANC Visit"
        val interpolatedButton = buttonProperties.interpolate(map)
        Assert.assertEquals("DUE", interpolatedButton.status)
        Assert.assertEquals("#FFA500", interpolatedButton.backgroundColor)
        Assert.assertEquals("true", interpolatedButton.enabled)
        Assert.assertEquals("ANC Visit", interpolatedButton.text)
    }
}