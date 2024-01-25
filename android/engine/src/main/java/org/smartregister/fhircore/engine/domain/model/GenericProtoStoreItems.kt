package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GenericProtoStoreItems(
  val careTeamIds: List<String>? = null,
  val careTeamNames: List<String>? = null,
  val locationIds: List<String>? = null,
  val locationNames: List<String>? = null,
  val organizationIds: List<String>? = null,
  val organizationNames: List<String>? = null
)
