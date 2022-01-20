package org.smartregister.fhircore.engine.nfc.main

import com.google.gson.annotations.SerializedName

data class PatientNfcItem(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("middle_name") val middleName: String,
    @SerializedName("age") val age: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("caretaker_name") val caretakerName: String,
    @SerializedName("caretaker_relationship") val caretakerRelationship: String,
    @SerializedName("village") val village: String,
    @SerializedName("health_center") val healthCenter: String,
    @SerializedName("beneficiary_group") val beneficiaryGroup: String,
    @SerializedName("registration_date") val registrationDate: String,
    @SerializedName("creation_date") val creationDate: String
)
