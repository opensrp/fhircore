package org.smartregister.fhircore.engine.data.domain.util

/**
 * Mapper util for transforming model of type [T] to [DomainModel]. An example is transforming FHIR
 * resource to an entity class
 */
interface DomainMapper<T, DomainModel> {

  fun mapToDomainModel(dto: T): DomainModel
}
