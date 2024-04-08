package org.smartregister.fhircore.engine.configuration.geowidget

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.RuleConfig

@Serializable
@Parcelize
data class SummaryBottomSheetConfig(val rules: List<RuleConfig>, val bottomSheetView : List<ViewProperties>, val servicePointID : String) :
    Parcelable