package org.smartregister.fhircore.anc.data.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository

//todo delete or merge or implement after discussion
class DummyFamilyRegisterRepository: RegisterRepository<Patient, FamilyItem> {
    override val domainMapper: DomainMapper<Patient, FamilyItem>
        get() = TODO("Not yet implemented")
    override val defaultPageSize: Int
        get() = TODO("Not yet implemented")
    override val fhirEngine: FhirEngine
        get() = TODO("Not yet implemented")

    override suspend fun loadData(
        query: String,
        pageNumber: Int,
        primaryFilterCallback: (Search) -> Unit,
        vararg secondaryFilterCallbacks: (String, Search) -> Unit,
    ): List<FamilyItem> {
        TODO("Not yet implemented")
    }
}