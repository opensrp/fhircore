package org.smartregister.fhircore.engine.configuration.register

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.smartregister.fhircore.engine.domain.model.DataQuery

@kotlinx.serialization.Serializable
@Parcelize
data class RegisterSearchConfig(
  val display: String? = null,
  val visible: Boolean? = null,
  val dataQueries: List<DataQuery>? = null
) : Parcelable