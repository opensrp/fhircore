package org.smartregister.fhircore.anc.data.family.model

import androidx.compose.runtime.Stable
import org.hl7.fhir.r4.model.Address

@Stable
data class FamilyItem(
  val id: String,
  val name: String,
  val gender: String,
  val age: String,
  val address: String,
  val isPregnant: Boolean,
  val members: List<FamilyMemberItem>,
  val servicesDue: Int,
  val servicesOverdue: Int
){
  fun extractDemographics(): String{
    return "$name, $gender, $age"
  }
}