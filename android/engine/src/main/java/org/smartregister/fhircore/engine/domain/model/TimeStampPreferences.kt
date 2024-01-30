package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType

/*
 * When working with the datastore, we extract the map, push a key value pair, then put back the map into a dataclass.
 * Using the data class allows for serialization and using the map allows for uniqueness of the resourceTypes in the datastore and updating the timestamp for a resource
 * */
@Serializable data class TimeStampPreferences(val map: Map<ResourceType, String>)
