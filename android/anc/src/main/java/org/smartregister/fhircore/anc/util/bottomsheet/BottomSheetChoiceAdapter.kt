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

package org.smartregister.fhircore.anc.util.bottomsheet

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject
import org.smartregister.fhircore.anc.databinding.ItemRowBinding

@FragmentScoped
class BottomSheetChoiceAdapter @Inject constructor(val onClickListener: OnClickListener) :
  ListAdapter<BottomSheetDataModel, BottomSheetChoiceAdapter.TestViewHolder>(
    BottomSheetDataModelItemDiffCallback
  ) {

  inner class TestViewHolder(private val containerView: ItemRowBinding) :
    RecyclerView.ViewHolder(containerView.root) {

    fun bindTo(model: BottomSheetDataModel) {
      containerView.userSelectionRadioButton.text = model.itemName
      containerView.textViewDetail.text = model.itemDetail
      containerView.userSelectionRadioButton.setOnClickListener {
        onClickListener.onClick(it as RadioButton, layoutPosition)
      }
      containerView.userSelectionRadioButton.isChecked = model.selected
    }
  }

  override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemRowBinding.inflate(inflater)
    return TestViewHolder(binding)
  }

  object BottomSheetDataModelItemDiffCallback : DiffUtil.ItemCallback<BottomSheetDataModel>() {
    override fun areItemsTheSame(oldItem: BottomSheetDataModel, newItem: BottomSheetDataModel) =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: BottomSheetDataModel, newItem: BottomSheetDataModel) =
      oldItem == newItem
  }
}

interface OnClickListener {
  fun onClick(rb: RadioButton, position: Int)
}

data class BottomSheetDataModel(
  val itemName: String,
  val itemDetail: String,
  val id: String,
  var selected: Boolean = false
)
