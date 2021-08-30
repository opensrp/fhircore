package org.smartregister.fhircore.engine.ui.register.model

import android.graphics.drawable.Drawable
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Patient

/**
 * @property itemId Unique number used to identify the menu item.
 * @property titleResource Android translatable string resource used as the menu option title
 * @property iconResource Android drawable resource used as icon for menu option
 * @property opensMainRegister Use current option to open the main the register
 * @property showCount Show clients count against the menu option
 * @property countForResource Indicate whether the count should be for FHIR resource
 * @property entityTypePatient Indicate whether the resource is Patient. Provide custom count
 * queries for resources other than Patient
 * @property searchFilterLambda Filter applied to the the counted entities. Default to filtering
 * only active Patients
 */
data class SideMenuOption(
  val itemId: Int,
  val titleResource: Int,
  val iconResource: Drawable,
  val opensMainRegister: Boolean = false,
  val showCount: Boolean = true,
  val countForResource: Boolean = true,
  val entityTypePatient: Boolean = true,
  val searchFilterLambda: (Search) -> Unit = { search -> search.filter(Patient.ACTIVE, true) }
)
