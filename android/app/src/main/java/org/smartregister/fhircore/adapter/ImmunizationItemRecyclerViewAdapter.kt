/*
 * Copyright 2021 Ona Systems Inc
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
import org.hl7.fhir.r4.model.Immunization
import org.smartregister.fhircore.R
import org.smartregister.fhircore.viewholder.ImmunizationItemViewHolder

/** UI Controller helper class to display list of Immunizations. */
class ImmunizationItemRecyclerViewAdapter :
  ListAdapter<Immunization, ImmunizationItemViewHolder>(ImmunizationDiffCallback()) {

  class ImmunizationDiffCallback : DiffUtil.ItemCallback<Immunization>() {
    override fun areItemsTheSame(oldItem: Immunization, newItem: Immunization): Boolean =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Immunization, newItem: Immunization): Boolean =
      oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImmunizationItemViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.observation_list_item, parent, false)
    return ImmunizationItemViewHolder(view)
  }

  override fun onBindViewHolder(holder: ImmunizationItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item)
  }
}
