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

package org.smartregister.fhircore.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.fragment.CovaxListFragment
import org.smartregister.fhircore.fragment.FamilyListFragment
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.model.PatientStatus
import org.smartregister.fhircore.model.VaccineStatus
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.util.Utils.getPatientAgeGender
import android.content.Context

import androidx.test.core.app.ApplicationProvider.getApplicationContext

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.test.core.app.ApplicationProvider
import org.smartregister.fhircore.R


class FamilyItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val demographics: TextView = itemView.findViewById(R.id.tv_family_demographics)
  private val area: TextView = itemView.findViewById(R.id.tv_area)
  private val membersContainer: LinearLayout = itemView.findViewById(R.id.family_members_container)

  fun bindTo(
    familyItem: FamilyItem,
    onItemClicked: (FamilyItem) -> Unit,
  ) {
    this.demographics.text = getDemographics(familyItem)
    area.text = familyItem.area
    membersContainer.removeAllViews()

    familyItem.members.forEach {
      inflateAndAddView(
        if(!it.pregnant.isNullOrEmpty()) {R.layout.family_member_mother_item}
        else R.layout.family_member_other_item
      , membersContainer)
    }
  }

  fun inflateAndAddView(@LayoutRes id: Int, root: ViewGroup) {
    val inflater = itemView.context.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val view = inflater.inflate(id, root, false)

    membersContainer.addView(view)
  }

  private fun getDemographics(familyItem: FamilyItem): String {
    val age = Utils.getAgeFromDate(familyItem.dob)
    val gender = if (familyItem.gender == "male") 'M' else 'F'

    val names = familyItem.name.split(' ')
    return listOf(names[1], names[0], gender, "$age").joinToString()
  }
}
