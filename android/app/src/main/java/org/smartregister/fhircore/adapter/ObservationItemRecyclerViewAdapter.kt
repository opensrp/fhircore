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
import org.smartregister.fhircore.viewholder.ObservationItemViewHolder
import org.smartregister.fhircore.viewmodel.PatientListViewModel

/** UI Controller helper class to display list of observations. */
class ObservationItemRecyclerViewAdapter :
  ListAdapter<PatientListViewModel.ObservationItem, ObservationItemViewHolder>(
    ObservationItemDiffCallback()
  ) {

  class ObservationItemDiffCallback :
    DiffUtil.ItemCallback<PatientListViewModel.ObservationItem>() {
    override fun areItemsTheSame(
      oldItem: PatientListViewModel.ObservationItem,
      newItem: PatientListViewModel.ObservationItem
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
      oldItem: PatientListViewModel.ObservationItem,
      newItem: PatientListViewModel.ObservationItem
    ): Boolean = oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObservationItemViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.observation_list_item, parent, false)
    return ObservationItemViewHolder(view)
  }

  override fun onBindViewHolder(holder: ObservationItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item)
  }
}
