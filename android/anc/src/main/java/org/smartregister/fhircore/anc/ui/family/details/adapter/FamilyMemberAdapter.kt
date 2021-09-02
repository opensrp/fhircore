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

package org.smartregister.fhircore.anc.ui.family.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.details.viewholder.AddMemberButtonViewHolder
import org.smartregister.fhircore.anc.ui.family.details.viewholder.FamilyMemberItemViewHolder

class FamilyMemberAdapter(
  private val onItemClicked: (FamilyMemberItem) -> Unit,
  private val onAddNewMemberButtonClicked: () -> Unit
) : ListAdapter<FamilyMemberItem, RecyclerView.ViewHolder>(FamilyMemberItemDiffCallback()) {

  class FamilyMemberItemDiffCallback : DiffUtil.ItemCallback<FamilyMemberItem>() {
    override fun areItemsTheSame(oldItem: FamilyMemberItem, newItem: FamilyMemberItem): Boolean =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FamilyMemberItem, newItem: FamilyMemberItem): Boolean =
      oldItem == newItem
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return when (viewType) {
      ADD_MEMBER_BUTTON_TYPE ->
        AddMemberButtonViewHolder(
          LayoutInflater.from(parent.context).inflate(R.layout.add_member_item, parent, false)
        )
      else ->
        FamilyMemberItemViewHolder(
          LayoutInflater.from(parent.context).inflate(R.layout.family_member_item, parent, false)
        )
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    when (holder.itemViewType) {
      ADD_MEMBER_BUTTON_TYPE -> {
        (holder as AddMemberButtonViewHolder).bind(onAddNewMemberButtonClicked)
      }
      MEMBER_TYPE -> {
        val item = currentList[position]
        (holder as FamilyMemberItemViewHolder).bindTo(item, onItemClicked)
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return if (isLastItem(position)) ADD_MEMBER_BUTTON_TYPE else MEMBER_TYPE
  }

  override fun getItemCount(): Int {
    return super.getItemCount() + 1
  }

  private fun isLastItem(position: Int): Boolean {
    return position == itemCount.minus(1)
  }

  companion object {
    const val MEMBER_TYPE = 0
    const val ADD_MEMBER_BUTTON_TYPE = 1
  }
}