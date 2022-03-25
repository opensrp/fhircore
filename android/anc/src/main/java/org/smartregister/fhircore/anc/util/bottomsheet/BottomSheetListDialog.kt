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
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.smartregister.fhircore.anc.R

class BottomSheetListDialog(
  @NonNull context: Context,
  private val bottomSheetHolder: BottomSheetHolder,
  private val selectedItem: (item: BottomSheetDataModel) -> Unit
) : BottomSheetDialog(context), OnClickListener {
  private lateinit var adapter: BottomSheetChoiceAdapter
  private lateinit var recyclerView: RecyclerView
  private lateinit var saveButton: Button
  private lateinit var cancelButton: Button
  private lateinit var tvTitle: TextView
  private lateinit var tvWarningTitle: TextView
  private lateinit var tvListLabel: TextView

  private val drawablePadding = 28 // default value

  init {
    initialize()
    setupList()
    setCancelable(false)
  }

  private fun initialize() {
    val contentView = View.inflate(context, R.layout.layout_bottom_sheet, null)
    setContentView(contentView)
    setupViews(contentView)
  }

  private fun setupViews(contentView: View) {
    recyclerView = contentView.findViewById(R.id.recyclerView)
    saveButton = contentView.findViewById(R.id.buttonSave)
    cancelButton = contentView.findViewById(R.id.buttonCancel)
    tvTitle = contentView.findViewById(R.id.tvTitle)
    tvWarningTitle = contentView.findViewById(R.id.tvWarningTitle)
    tvListLabel = contentView.findViewById(R.id.tvListLabel)

    tvTitle.text = bottomSheetHolder.title
    tvListLabel.text = bottomSheetHolder.subTitle
    tvWarningTitle.text = bottomSheetHolder.tvWarningTitle

    saveButton.setOnClickListener {}

    cancelButton.setOnClickListener { dismiss() }
  }

  private fun setupList() {
    adapter = BottomSheetChoiceAdapter(this)
    recyclerView.run {
      adapter = this@BottomSheetListDialog.adapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
    adapter.setDataSource(bottomSheetHolder.list)
  }

  private fun onItemClicked(onClickListItems: OnClickedListItems, position: Int) {
    dismiss()
    onClickListItems.onClickedItem(position)
  }

  interface OnClickedListItems {
    fun onClickedItem(position: Int)

    fun onBottomSheetBehavior(type: Int)

    fun onSlide(bottomSheet: View, slideOffset: Float)
  }

  override fun onClick(rb: RadioButton, position: Int) {
    bottomSheetHolder.list.forEach { it.selected = false }
    bottomSheetHolder.list[position].selected = true
    adapter.notifyDataSetChanged()
  }
}

data class BottomSheetHolder(
  val title: String,
  val subTitle: String,
  val tvWarningTitle: String,
  val list: List<BottomSheetDataModel>
)
