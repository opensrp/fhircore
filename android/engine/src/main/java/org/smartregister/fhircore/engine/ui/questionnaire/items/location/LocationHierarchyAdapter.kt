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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.smartregister.fhircore.engine.domain.model.LocationHierarchy

class LocationHierarchyAdapter(
  context: Context,
  locations: List<LocationHierarchy>,
) : ArrayAdapter<LocationHierarchy>(context, 0, locations) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view =
      convertView
        ?: LayoutInflater.from(context)
          .inflate(com.google.android.fhir.datacapture.R.layout.drop_down_list_item, parent, false)
    val textView =
      view.findViewById<TextView>(com.google.android.fhir.datacapture.R.id.answer_option_textview)

    val location = getItem(position)
    textView.text = location?.name ?: ""

    return view
  }

  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view =
      convertView
        ?: LayoutInflater.from(context)
          .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
    val textView = view.findViewById<TextView>(android.R.id.text1)

    val location = getItem(position)
    textView.text = location?.name ?: ""

    return view
  }

  fun updateLocations(locations: List<LocationHierarchy>) {
    clear()
    addAll(locations)
    notifyDataSetChanged()
  }
}
