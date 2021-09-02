package org.smartregister.fhircore.anc.data.family.model

import androidx.compose.runtime.Stable

@Stable
data class FamilyMemberItem(
    val id: String,
    val age: String,
    val gender: String,
    val pregnant: Boolean
)
