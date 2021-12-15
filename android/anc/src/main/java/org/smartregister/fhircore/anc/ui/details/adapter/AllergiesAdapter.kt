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

package org.smartregister.fhircore.anc.ui.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject
import org.smartregister.fhircore.anc.data.model.AllergiesItem
import org.smartregister.fhircore.anc.databinding.ItemPlanTextBinding
import org.smartregister.fhircore.engine.ui.base.BaseSimpleRecyclerViewHolder

/** Subclass of [ListAdapter] used to display allergies for the non ANC client */
@FragmentScoped
class AllergiesAdapter @Inject constructor() :
  ListAdapter<AllergiesItem, AllergiesAdapter.PatientAllergiesViewHolder>(
    AllergiesItemDiffCallback
  ) {

  inner class PatientAllergiesViewHolder(private val containerView: ItemPlanTextBinding) :
    BaseSimpleRecyclerViewHolder<AllergiesItem>(containerView.root) {
    override fun bindTo(data: AllergiesItem) {
      with(data) { containerView.title = title }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientAllergiesViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemPlanTextBinding.inflate(inflater)
    return PatientAllergiesViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PatientAllergiesViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  object AllergiesItemDiffCallback : DiffUtil.ItemCallback<AllergiesItem>() {
    override fun areItemsTheSame(oldItem: AllergiesItem, newItem: AllergiesItem) =
      oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: AllergiesItem, newItem: AllergiesItem) =
      oldItem == newItem
  }
}
