package org.smartregister.fhircore.engine.data.domain.util

/** Mapper util for transforming model of type [T] to [DomainModel] and vice versa */
interface DomainMapper<T, DomainModel> {

  fun mapToDomainModel(dto: T): DomainModel

  fun mapFromDomainModel(domainModel: DomainModel): T
}
