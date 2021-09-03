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

package org.smartregister.fhircore.anc.ui.family.details.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem

class FamilyMemberItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val demographics: TextView = itemView.findViewById(R.id.tv_member_demographics)

  fun bindTo(
    familyMemberItem: FamilyMemberItem,
    onItemClicked: (FamilyMemberItem) -> Unit,
  ) {

    demographics.text = getDemographics(familyMemberItem)
    itemView.setOnClickListener { onItemClicked(familyMemberItem) }
  }

  private fun getDemographics(familyMemberItem: FamilyMemberItem): String {
    val age = familyMemberItem.age
    val gender = if (familyMemberItem.gender == "male") 'M' else 'F'

    val names = familyMemberItem.name.split(' ')
    return listOf(names[1], names[0], gender, "$age").joinToString()
  }
}
