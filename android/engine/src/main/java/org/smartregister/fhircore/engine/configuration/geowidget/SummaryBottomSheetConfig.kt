package org.smartregister.fhircore.engine.configuration.geowidget

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.view.ViewProperties

@Serializable
@Parcelize
data class SummaryBottomSheetConfig(val views: List<ViewProperties>? = emptyList()) : Parcelable