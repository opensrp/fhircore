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
import androidx.recyclerview.widget.RecyclerView
import org.hl7.fhir.r4.model.CarePlan
import org.smartregister.fhircore.anc.databinding.ItemCareplanBinding
import java.util.*

/** Subclass of [ListAdapter] used to display careplan for the ANC client */
class CarePlanAdapter :
    ListAdapter<CarePlan, CarePlanAdapter.PatientImmunizationsViewHolder>(
        ImmunizationItemDiffCallback
    ) {

    inner class PatientImmunizationsViewHolder(private val containerView: ItemCareplanBinding) :
        RecyclerView.ViewHolder(containerView.root) {
        fun bindTo(immunizationItem: CarePlan) {
            with(immunizationItem) {
                val datePassed = this.period.start.before(Date())
                containerView.carPlanDatePassed = datePassed
                containerView.carPlanTitle =
                    if (datePassed) this.title + " Overdue" else this.title

            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PatientImmunizationsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCareplanBinding.inflate(inflater)
        return PatientImmunizationsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientImmunizationsViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    object ImmunizationItemDiffCallback : DiffUtil.ItemCallback<CarePlan>() {
        override fun areItemsTheSame(oldItem: CarePlan, newItem: CarePlan) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CarePlan, newItem: CarePlan) =
            oldItem.equals(newItem)
    }
}
