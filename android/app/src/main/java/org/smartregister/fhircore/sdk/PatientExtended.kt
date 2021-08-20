package org.smartregister.fhircore.sdk

import ca.uhn.fhir.model.api.annotation.SearchParamDefinition
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.UriClientParam
import org.hl7.fhir.r4.model.Patient

class PatientExtended : Patient() {
  @SearchParamDefinition(
    name = "tag",
    path = "Patient.meta.tag.display",
    description = "Tag",
    type = "string"
  )
  @JvmField
  val SP_TAG = TAG_KEY

  @SearchParamDefinition(
    name = "profile",
    path = "Patient.meta.profile",
    description = "Profile",
    type = "string"
  )
  @JvmField
  val SP_PROFILE = PROFILE_KEY

  companion object {
    const val TAG_KEY = "tag"
    const val PROFILE_KEY = "profile"

    val TAG = StringClientParam(TAG_KEY)
    val PROFILE = StringClientParam(PROFILE_KEY)
  }
}
