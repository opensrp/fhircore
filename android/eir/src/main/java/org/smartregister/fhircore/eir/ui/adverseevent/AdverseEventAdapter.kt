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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.eir.databinding.AdverseEventListItemBinding
import org.smartregister.fhircore.eir.ui.adverseevent.AdverseEventAdapter.AdverseEventViewHolder
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem

class AdverseEventAdapter(val vaccine: String, val adverseEventDoseNumber: List<String>) :
  ListAdapter<AdverseEventItem, AdverseEventViewHolder>(AdverseEventItemDiffCallback) {

  inner class AdverseEventViewHolder(private val binding: AdverseEventListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bindTo(immunizationAdverseEventItem: AdverseEventItem, position: Int) {
      with(immunizationAdverseEventItem) {
        binding.root.tag = this
        binding.vaccine = vaccine + " - Dose "+  adverseEventDoseNumber[position]
        binding.immunizationItem = immunizationAdverseEventItem
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdverseEventViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = AdverseEventListItemBinding.inflate(inflater)
    return AdverseEventViewHolder(binding)
  }

  override fun onBindViewHolder(holder: AdverseEventViewHolder, position: Int) {
    holder.bindTo(getItem(position),position)
  }

  object AdverseEventItemDiffCallback : DiffUtil.ItemCallback<AdverseEventItem>() {
    override fun areItemsTheSame(oldItem: AdverseEventItem, newItem: AdverseEventItem) =
      oldItem.date == newItem.date

    override fun areContentsTheSame(oldItem: AdverseEventItem, newItem: AdverseEventItem) =
      oldItem == newItem
  }
}
