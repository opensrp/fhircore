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

package org.smartregister.fhircore.anc.ui.madx.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.anc.data.madx.model.ConditionItem
import org.smartregister.fhircore.anc.databinding.ItemEncountersBinding
import org.smartregister.fhircore.anc.databinding.ItemPlanTextBinding
import org.smartregister.fhircore.anc.databinding.ItemServicesBinding

/** Subclass of [ListAdapter] used to display conditions for the non ANC client */
class ConditionsAdapter :
  ListAdapter<ConditionItem, ConditionsAdapter.PatientConditionViewHolder>(
    ImmunizationItemDiffCallback
  ) {

  inner class PatientConditionViewHolder(private val containerView: ItemPlanTextBinding) :
    RecyclerView.ViewHolder(containerView.root) {
    fun bindTo(conditionItem: ConditionItem) {
      with(conditionItem) {
        containerView.title = title
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientConditionViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemPlanTextBinding.inflate(inflater)
    return PatientConditionViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PatientConditionViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  object ImmunizationItemDiffCallback : DiffUtil.ItemCallback<ConditionItem>() {
    override fun areItemsTheSame(oldItem: ConditionItem, newItem: ConditionItem) =
      oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: ConditionItem, newItem: ConditionItem) =
      oldItem.equals(newItem)
  }
}
