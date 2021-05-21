package org.smartregister.fhircore.fragment

/**
 * Created by Vincent Karuri on 19/05/2021
 */
interface OnPatientSearchResult {
    fun onSearchDone(isPatientFound: Boolean, patientLogicalId: String)
}