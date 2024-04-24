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

class LocationPickerView(
  private val context: Context,
  itemView: View,
  private val lifecycleScope: LifecycleCoroutineScope,
) {
  private var customQuestItemDataProvider: CustomQuestItemDataProvider? = null
  private var rootLayout: LinearLayout? = null
  private val dropdownMap = mutableMapOf<String, AutoCompleteTextView>()

  private var selectedHierarchy: LocationHierarchy? = null
  private var physicalLocator: String? = null

  private var onLocationChanged: ((StringType?) -> Unit)? = null

  private var cardView: CardView? = null
  private var locationText: TextView? = null
  private var textInputLayout: TextInputLayout? = null
  private var textInputEditText: TextInputEditText? = null
  var headerView: HeaderView? = null

  init {
    cardView = itemView.findViewById(R.id.location_picker_view)
    locationText = cardView?.findViewById(R.id.location)
    headerView = itemView.findViewById(R.id.header)
    textInputLayout = itemView.findViewById(R.id.text_input_layout)
    textInputEditText = itemView.findViewById(R.id.text_input_edit_text)

    cardView?.setOnClickListener { showDropdownDialog() }
    textInputEditText?.doAfterTextChanged { editable: Editable? ->
      lifecycleScope.launch {
        physicalLocator = editable.toString()
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
    locationText?.text = selectedHierarchy?.name ?: "-"
  }

  private fun onUpdate() {
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
      updateLocationData(locations = locations)
    }
  }

  private fun updateLocationData(locations: List<LocationHierarchy>) {
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
        updateLocationData(selectedLocation.children)
      }
    } else if (selectedLocation != null) {
      this.selectedHierarchy = selectedLocation
    }
  }

  fun setCustomDataProvider(customQuestItemDataProvider: CustomQuestItemDataProvider) {
    this.customQuestItemDataProvider = customQuestItemDataProvider
  }
}
