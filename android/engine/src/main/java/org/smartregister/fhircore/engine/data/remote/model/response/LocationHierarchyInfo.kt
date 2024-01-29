package org.smartregister.fhircore.engine.data.remote.model.response

import kotlinx.serialization.Serializable
/**
 * This data class holds the members of the LocationHierarchy class that we need to store in the Proto Datastore.
 * It is similar to the PractitionerInfo data class
 **/
@Serializable
data class LocationHierarchyInfo(
  val x: Int = 1
)
