package org.smartregister.fhircore.anc.sdk

import ca.uhn.fhir.model.api.annotation.SearchParamDefinition
import ca.uhn.fhir.rest.gclient.StringClientParam
import org.hl7.fhir.r4.model.Patient

class PatientExtended : Patient() {
  @SearchParamDefinition(
    name = TAG_KEY,
    path = "Patient.meta.tag.display",
    description = "Tag",
    type = "string"
  )
  @JvmField
  val SP_TAG = TAG_KEY

  companion object {
    const val TAG_KEY = "_tag"
    val TAG = StringClientParam(TAG_KEY)


  }
}