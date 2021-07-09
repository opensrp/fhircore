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

package org.smartregister.fhircore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientDetailsCard
import org.smartregister.fhircore.viewholder.PatientDetailsCardViewHolder

/** UI Controller helper class to display list of observations. */
class PatientDetailsCardRecyclerViewAdapter :
  ListAdapter<PatientDetailsCard, PatientDetailsCardViewHolder>(PatientDetailsCardDiffCallback()) {

  class PatientDetailsCardDiffCallback : DiffUtil.ItemCallback<PatientDetailsCard>() {
    override fun areItemsTheSame(
      oldItem: PatientDetailsCard,
      newItem: PatientDetailsCard
    ): Boolean = oldItem.id == newItem.id && oldItem.type == newItem.type

    override fun areContentsTheSame(
      oldItem: PatientDetailsCard,
      newItem: PatientDetailsCard
    ): Boolean = oldItem == newItem
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientDetailsCardViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.patient_details_card_item, parent, false)
    return PatientDetailsCardViewHolder(view)
  }

  override fun onBindViewHolder(holder: PatientDetailsCardViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item)
  }
}
