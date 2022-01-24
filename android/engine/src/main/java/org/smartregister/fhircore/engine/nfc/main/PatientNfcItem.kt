package org.smartregister.fhircore.engine.nfc.main

data class PatientNfcItem(
    val patientId: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
    val age: String,
    val birthDate: String,
    val gender: String,
    val caretakerName: String,
    val caretakerRelationship: String,
    val village: String,
    val healthCenter: String,
    val beneficiaryGroup: String,
    val registrationDate: String,
    val creationDate: String
)
