package org.smartregister.fhircore.quest.ui.launcher


sealed class GeoWidgetEvent {

    data class SearchServicePoints(val searchText: String = "") : GeoWidgetEvent()

    object Toggle : GeoWidgetEvent()

}