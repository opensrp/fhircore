/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.ui.questionnaire.items.location

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.LocationHierarchy
import org.smartregister.fhircore.engine.ui.questionnaire.items.CustomQuestItemDataProvider
import timber.log.Timber

class LocationPickerView(
  private val context: Context,
  itemView: View,
  private val lifecycleScope: LifecycleCoroutineScope,
) {
  private var customQuestItemDataProvider: CustomQuestItemDataProvider? = null
  private var rootLayout: LinearLayout? = null
  private val dropdownMap = mutableMapOf<String, AutoCompleteTextView>()

  private var selectedHierarchy: LocationData? = null
  private var physicalLocator: String? = null

  private var onLocationChanged: ((StringType?) -> Unit)? = null

  private var cardView: CardView? = null
  private var locationNameText: TextView? = null
  private var physicalLocatorInputLayout: TextInputLayout? = null
  private var physicalLocatorInputEditText: TextInputEditText? = null
  var headerView: HeaderView? = null

  val helperText: TextView
  private var errorView: LinearLayout
  private var errorText: TextView

  private var initialValue: String? = null

  init {
    cardView = itemView.findViewById(R.id.location_picker_view)
    locationNameText = cardView?.findViewById(R.id.location)
    headerView = itemView.findViewById(R.id.header)
    physicalLocatorInputLayout = itemView.findViewById(R.id.physical_locator_input_layout)
    physicalLocatorInputEditText = itemView.findViewById(R.id.physical_locator_edit_text)

    helperText = itemView.findViewById(R.id.location_helper_text)
    errorView = itemView.findViewById(R.id.item_error_view)
    errorText = itemView.findViewById(R.id.error_text)

    cardView?.setOnClickListener { showDropdownDialog() }
    physicalLocatorInputEditText?.doAfterTextChanged { editable: Editable? ->
      lifecycleScope.launch {
        physicalLocator = editable.toString().ifBlank { null }
        onUpdate()
      }
    }
  }

  fun setOnLocationChanged(listener: ((StringType?) -> Unit)?) {
    onLocationChanged = listener
  }

  fun setEnabled(enabled: Boolean) {
    cardView?.isEnabled = enabled
  }

  private fun showDropdownDialog() {
    val dialogView =
      LayoutInflater.from(context).inflate(R.layout.custom_location_picker_view, null)

    val builder = AlertDialog.Builder(context)
    builder.setOnCancelListener { resetState() }
    builder.setPositiveButton("Select") { _, _ ->
      onLocationSelected()
      resetState()
    }
    builder.setNegativeButton("Cancel") { _, _ -> resetState() }

    builder.setView(dialogView)

    rootLayout = dialogView.findViewById(R.id.location_picker_view)
    val dialog = builder.create()
    initData()
    dialog.show()
  }

  private fun onLocationSelected() {
    onUpdate()
    locationNameText?.text = selectedHierarchy?.name ?: "-"
  }

  private fun onUpdate() {
    Timber.e(physicalLocator)
    if (physicalLocator == null || selectedHierarchy == null) {
      onLocationChanged?.invoke(StringType(null))
      return
    }
    val strValue =
      "${selectedHierarchy?.identifier ?: "-"}|${selectedHierarchy?.name ?: "-"}|${physicalLocator ?: "-"}"
    onLocationChanged?.invoke(StringType(strValue))
  }

  private fun resetState() {
    dropdownMap.clear()
  }

  private fun initData() {
    customQuestItemDataProvider?.let {
      val locations = it.fetchLocationHierarchies()
      updateLocationData(locations = locations, isDefault = true)
    }
  }

  private fun updateLocationData(
    locations: List<LocationHierarchy>,
    isDefault: Boolean = false,
    parent: LocationHierarchy? = null,
  ) {
    rootLayout?.let { rootLayout ->
      val mainLayout =
        LayoutInflater.from(context).inflate(R.layout.custom_material_spinner, rootLayout, false)
      val dropdown = mainLayout.findViewById<MaterialAutoCompleteTextView>(R.id.menu_auto_complete)
      val layoutParams =
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
        )
      layoutParams.bottomMargin = 16
      mainLayout.layoutParams = layoutParams

      if (parent != null) {
        val helperText = mainLayout.findViewById<TextView>(R.id.helper_text)
        helperText.visibility = View.VISIBLE
        helperText.text = context.getString(R.string.select_locations_in, parent.name)
      }

      val adapter = LocationHierarchyAdapter(context, locations)
      dropdown.setAdapter(adapter)

      dropdown.setOnItemClickListener { _, _, position, _ ->
        val selectedLocation = adapter.getItem(position)
        onOptionSelected(selectedLocation, dropdown)
      }

      rootLayout.addView(mainLayout)

      if (locations.size == 1) {
        val selected = locations.first()
        dropdown.setText(selected.name, false)
        onOptionSelected(selected, dropdown)
        if (isDefault) {
          dropdown.isEnabled = false
        }
      }
    }
  }

  private fun onOptionSelected(
    selectedLocation: LocationHierarchy?,
    dropdown: AutoCompleteTextView,
  ) {
    if (selectedLocation != null && selectedLocation.children.isNotEmpty()) {
      if (dropdownMap.containsKey(selectedLocation.identifier)) {
        (dropdownMap[selectedLocation.identifier]?.adapter as LocationHierarchyAdapter?)
          ?.updateLocations(selectedLocation.children)
      } else {
        dropdownMap[selectedLocation.identifier] = dropdown
        updateLocationData(selectedLocation.children, parent = selectedLocation)
      }
    } else if (selectedLocation != null) {
      this.selectedHierarchy = LocationData.fromHierarchy(selectedLocation)
    }
  }

  fun setCustomDataProvider(customQuestItemDataProvider: CustomQuestItemDataProvider) {
    this.customQuestItemDataProvider = customQuestItemDataProvider
  }

  fun initLocation(initialAnswer: String?) {
    Timber.e("$initialAnswer $initialValue")
    if (initialAnswer != null && initialValue == null) {
      val elements = initialAnswer.split("|")
      val locationId = elements.getOrNull(0)
      val locationName =
        elements.getOrNull(1)?.let {
          locationNameText?.text = it
          it
        }
      if (locationId != null && locationName != null) {
        selectedHierarchy = LocationData(locationId, locationName)
      }
      elements.getOrNull(2)?.let {
        physicalLocator = it
        if (it != "-") {
          physicalLocatorInputEditText?.setText(it)
        }
      }
      initialValue = initialAnswer
    }
  }

  fun showError(validationErrorMessage: String?) {
    if (validationErrorMessage == null) {
      errorView.visibility = View.GONE
      return
    }

    errorView.visibility = View.VISIBLE
    errorText.text = validationErrorMessage
  }

  fun setRequiredOrOptionalText(requiredOrOptionalText: String?) {
    physicalLocatorInputLayout?.let { it.helperText = requiredOrOptionalText }
    if (requiredOrOptionalText == null) {
      helperText.visibility = View.GONE
      return
    }
    helperText.text = requiredOrOptionalText
    helperText.visibility = View.VISIBLE
  }
}

data class LocationData(val identifier: String, val name: String) {
  companion object {
    fun fromHierarchy(location: LocationHierarchy): LocationData {
      return LocationData(location.identifier, location.name)
    }
  }
}
