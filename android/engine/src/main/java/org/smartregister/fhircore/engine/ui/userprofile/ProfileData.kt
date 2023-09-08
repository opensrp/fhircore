package org.smartregister.fhircore.engine.ui.userprofile

import org.smartregister.model.practitioner.PractitionerDetails

data class ProfileData(
    val userName: String,
    val locationName: String?,
    val organisation: String?,
    val isUserValid: Boolean,
    val practitionerDetails: PractitionerDetails?
)