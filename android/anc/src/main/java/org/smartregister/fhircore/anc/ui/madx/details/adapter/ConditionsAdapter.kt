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
import org.smartregister.fhircore.anc.data.sharedmodel.ConditionItem
import org.smartregister.fhircore.anc.databinding.ItemPlanTextBinding
import org.smartregister.fhircore.engine.ui.base.BaseSimpleRecyclerViewHolder

/** Subclass of [ListAdapter] used to display conditions for the non ANC client */
class ConditionsAdapter :
  ListAdapter<ConditionItem, ConditionsAdapter.PatientConditionViewHolder>(
    ConditionItemDiffCallback
  ) {

  inner class PatientConditionViewHolder(private val containerView: ItemPlanTextBinding) :
    BaseSimpleRecyclerViewHolder<ConditionItem>(containerView.root) {
    override fun bindTo(data: ConditionItem) {
      with(data) { containerView.title = title }
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

  object ConditionItemDiffCallback : DiffUtil.ItemCallback<ConditionItem>() {
    override fun areItemsTheSame(oldItem: ConditionItem, newItem: ConditionItem) =
      oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: ConditionItem, newItem: ConditionItem) =
      oldItem.equals(newItem)
  }
}
