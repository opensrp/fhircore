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

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.model.ImmunizationItem

/**
 * Subclass of [ListAdapter] used to display vaccine doses for the patient
 */
class PatientImmunizationsAdapter :
    ListAdapter<ImmunizationItem, PatientImmunizationsAdapter.PatientImmunizationsViewHolder>(
        ImmunizationItemDiffCallback
    ) {

    inner class PatientImmunizationsViewHolder(private val containerView: View) :
        RecyclerView.ViewHolder(containerView) {
        fun bindTo(immunizationItem: ImmunizationItem) {
            with(immunizationItem) {
                containerView.tag = this
                containerView.findViewById<TextView>(R.id.vaccineNameTextView).text = vaccine
                val vaccineDosesLayout =
                    containerView.findViewById<LinearLayout>(R.id.vaccineDosesLayout)
                addVaccineDoseViews(vaccineDosesLayout, doses)
            }
        }

        private fun addVaccineDoseViews(
            vaccineDosesLayout: LinearLayout, doses: List<Pair<String, Int>>,
        ) {
            vaccineDosesLayout.removeAllViews()
            doses.forEach { dose ->
                val (vaccineName, vaccineColorCode) = dose
                val doseTextView = TextView(vaccineDosesLayout.context).apply {
                    setTextColor(
                        ContextCompat.getColor(
                            vaccineDosesLayout.context,
                            vaccineColorCode
                        )
                    )
                    setPadding(0, 0, 0, 16)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP,18f);

                    text = vaccineName
                }
                vaccineDosesLayout.addView(doseTextView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : PatientImmunizationsViewHolder {
        val containerView = LayoutInflater.from(parent.context)
            .inflate(R.layout.immunization_list_item, parent, false)
        return PatientImmunizationsViewHolder(containerView)
    }

    override fun onBindViewHolder(holder: PatientImmunizationsViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    object ImmunizationItemDiffCallback : DiffUtil.ItemCallback<ImmunizationItem>() {
        override fun areItemsTheSame(
            oldItem: ImmunizationItem, newItem: ImmunizationItem
        ) = oldItem.vaccine == newItem.vaccine

        override fun areContentsTheSame(
            oldItem: ImmunizationItem, newItem: ImmunizationItem
        ) = oldItem == newItem
    }
}
