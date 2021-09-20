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

package org.smartregister.fhircore.anc.ui.madx.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Date
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.databinding.ItemCareplanBinding

/** Subclass of [ListAdapter] used to display careplan for the ANC client */
class CarePlanAdapter :
  ListAdapter<CarePlanItem, CarePlanAdapter.PatientCarePlanViewHolder>(
    ImmunizationItemDiffCallback
  ) {

  inner class PatientCarePlanViewHolder(private val containerView: ItemCareplanBinding) :
    RecyclerView.ViewHolder(containerView.root) {
    fun bindTo(carePlanItem: CarePlanItem) {
      with(carePlanItem) {
        val datePassed = this.periodStartDate.before(Date())
        containerView.carPlanDatePassed = datePassed
        containerView.carPlanTitle = if (datePassed) this.title + " Overdue" else this.title
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientCarePlanViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemCareplanBinding.inflate(inflater)
    return PatientCarePlanViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PatientCarePlanViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  object ImmunizationItemDiffCallback : DiffUtil.ItemCallback<CarePlanItem>() {
    override fun areItemsTheSame(oldItem: CarePlanItem, newItem: CarePlanItem) =
      oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: CarePlanItem, newItem: CarePlanItem) =
      oldItem.equals(newItem)
  }
}
