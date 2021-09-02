package org.smartregister.fhircore.anc.data.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource

class FamilyPaginatedDataSource(
    private val familyTag: String,
    val fhirEngine: FhirEngine,
    val domainMapper: DomainMapper<Family, FamilyItem>):
    PaginatedDataSource<Family, FamilyItem>(FamilyPaginatedRepository(fhirEngine, domainMapper)) {

    var query: String = ""

    override suspend fun loadData(pageNumber: Int): List<FamilyItem> {
        return registerRepository.loadData(
            pageNumber = pageNumber,
            query = query,
            /* todo primaryFilterCallback = { search: Search -> search.filter(PatientExtended.TAG){
               modifier = StringFilterModifier.MATCHES_EXACTLY
               value = familyTag
             } },*/
            primaryFilterCallback = { search: Search -> search.filter(Patient.ADDRESS_CITY){
                modifier = StringFilterModifier.CONTAINS
                value = "NAI"
            } },
            secondaryFilterCallbacks =
            arrayOf({ filterQuery: String, search: Search ->
                search.filter(Patient.NAME) {
                    modifier = StringFilterModifier.CONTAINS
                    value = filterQuery.trim()
                }
            })
        )
    }
}