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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.eir.databinding.ItemAdverseEventMainBinding
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.ImmunizationAdverseEventItem

class MainAdverseEventAdapter :
  ListAdapter<ImmunizationAdverseEventItem, MainAdverseEventAdapter.MainAdverseEventViewHolder>(
    AdverseEventItemDiffCallback
  ) {

  inner class MainAdverseEventViewHolder(private val binding: ItemAdverseEventMainBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bindTo(immunizationAdverseEventItem: ImmunizationAdverseEventItem, position: Int) {
      with(immunizationAdverseEventItem) {
        binding.root.tag = this
        val childMembersAdapter = AdverseEventAdapter(immunizationAdverseEventItem.vaccine, getAdverseEventDoseNumber(immunizationAdverseEventItem.dosesWithAdverseEvents)[position])
        childMembersAdapter.submitList(
          getAdverseEvent(immunizationAdverseEventItem.dosesWithAdverseEvents)
        )
        binding.adverseEventListView.layoutManager =
          LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
        binding.adverseEventListView.adapter = childMembersAdapter
      }
    }
  }

  private fun getAdverseEvent(
    dosesWithAdverseEvents: List<Pair<String, List<AdverseEventItem>>>
  ): List<AdverseEventItem> {
    val listOfAdverseEvent = arrayListOf<AdverseEventItem>()
    dosesWithAdverseEvents.forEach { item ->
      item.second.forEach { adverseEventReactionItem ->
        listOfAdverseEvent.add(adverseEventReactionItem)
      }
    }
    return listOfAdverseEvent
  }

  private fun getAdverseEventDoseNumber(
    dosesWithAdverseEvents: List<Pair<String, List<AdverseEventItem>>>
  ): List<String> {
    val listOfAdverseEvent = arrayListOf<String>()
    dosesWithAdverseEvents.forEach { item ->
      item.first.forEach { adverseEventReactionItem ->
        listOfAdverseEvent.add(adverseEventReactionItem.toString())
      }
    }
    return listOfAdverseEvent
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainAdverseEventViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemAdverseEventMainBinding.inflate(inflater)
    return MainAdverseEventViewHolder(binding)
  }

  override fun onBindViewHolder(holder: MainAdverseEventViewHolder, position: Int) {
    holder.bindTo(getItem(position),position)
  }

  object AdverseEventItemDiffCallback : DiffUtil.ItemCallback<ImmunizationAdverseEventItem>() {
    override fun areItemsTheSame(
      oldItem: ImmunizationAdverseEventItem,
      newItem: ImmunizationAdverseEventItem
    ) = oldItem.vaccine == newItem.vaccine

    override fun areContentsTheSame(
      oldItem: ImmunizationAdverseEventItem,
      newItem: ImmunizationAdverseEventItem
    ) = oldItem == newItem
  }
}
