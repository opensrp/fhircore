package org.smartregister.fhircore.engine.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class NotificationData(
    val title: String,
    val description: String,
    val type: String,
) : Parcelable, java.io.Serializable
