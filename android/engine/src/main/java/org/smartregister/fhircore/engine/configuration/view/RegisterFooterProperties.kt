package org.smartregister.fhircore.engine.configuration.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RegisterFooterProperties(
    val bottomPadding: Int = 32
) : Parcelable
