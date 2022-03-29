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

import android.content.Context
import android.widget.RadioButton
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.databinding.LayoutBottomSheetBinding

class BottomSheetListDialog(
  @NonNull context: Context,
  private val bottomSheetHolder: BottomSheetHolder,
  private val onBottomSheetListener: OnClickedListItems
) : BottomSheetDialog(context), OnClickListener {
  lateinit var binding: LayoutBottomSheetBinding
  private lateinit var adapter: BottomSheetChoiceAdapter
  private var selectedItem: BottomSheetDataModel? = null

  init {
    initialize()
    setupList()
    setCancelable(false)
  }

  private fun initialize() {
    binding = DataBindingUtil.inflate(layoutInflater, R.layout.layout_bottom_sheet, null, false)
    setContentView(binding.root)
    setupViews()
  }

  private fun setupViews() {
    binding.apply {
      tvTitle.text = bottomSheetHolder.title
      tvListLabel.text = bottomSheetHolder.subTitle
      layoutWarning.tvWarningTitle.text = bottomSheetHolder.tvWarningTitle
    }
    setupClickListener()
  }

  private fun setupClickListener() {
    binding.buttonSave.setOnClickListener {
      dismiss()
      selectedItem?.let { onBottomSheetListener.onSave(it) }
    }

    binding.buttonCancel.setOnClickListener { onBottomSheetListener.onCancel() }
  }

  private fun setupList() {
    adapter = BottomSheetChoiceAdapter(this)
    binding.recyclerView.apply {
      adapter = adapter
      layoutManager = LinearLayoutManager(context)
    }
    adapter.submitList(bottomSheetHolder.list)
  }

  interface OnClickedListItems {
    fun onSave(bottomSheetDataModel: BottomSheetDataModel)
    fun onCancel()
  }

  override fun onClick(rb: RadioButton, position: Int) {
    // refresh the list
    bottomSheetHolder.list.forEach { it.selected = false }
    bottomSheetHolder.list[position].selected = true
    // hold the selected item
    selectedItem = bottomSheetHolder.list[position]
    // enable the save button
    binding.buttonSave.isEnabled = true
    binding.buttonSave.backgroundTintList =
      ContextCompat.getColorStateList(context, R.color.colorPrimary)
    // notify adapter
    adapter.notifyItemChanged(position)
  }
}

data class BottomSheetHolder(
  val title: String,
  val subTitle: String,
  val tvWarningTitle: String,
  val list: List<BottomSheetDataModel>
)
