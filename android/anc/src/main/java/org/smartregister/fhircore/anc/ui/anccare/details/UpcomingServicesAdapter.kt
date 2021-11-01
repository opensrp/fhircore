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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.databinding.ItemServicesBinding
import org.smartregister.fhircore.engine.ui.base.BaseSimpleRecyclerViewHolder

/** Subclass of [ListAdapter] used to display careplan for the ANC client */
class UpcomingServicesAdapter :
  ListAdapter<UpcomingServiceItem, UpcomingServicesAdapter.PatientUpcomingServiceViewHolder>(
    UpcomingServiceItemDiffCallback
  ) {

  inner class PatientUpcomingServiceViewHolder(private val containerView: ItemServicesBinding) :
    BaseSimpleRecyclerViewHolder<UpcomingServiceItem>(containerView.root) {
    override fun bindTo(data: UpcomingServiceItem) {
      with(data) {
        containerView.title = title
        containerView.date = date
      }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): PatientUpcomingServiceViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemServicesBinding.inflate(inflater)
    return PatientUpcomingServiceViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PatientUpcomingServiceViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  object UpcomingServiceItemDiffCallback : DiffUtil.ItemCallback<UpcomingServiceItem>() {
    override fun areItemsTheSame(oldItem: UpcomingServiceItem, newItem: UpcomingServiceItem) =
      oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: UpcomingServiceItem, newItem: UpcomingServiceItem) =
      oldItem.equals(newItem)
  }
}
