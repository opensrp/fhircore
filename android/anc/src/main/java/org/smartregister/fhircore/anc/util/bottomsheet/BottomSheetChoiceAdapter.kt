package org.smartregister.fhircore.anc.util.bottomsheet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.anc.R

class BottomSheetChoiceAdapter(
    private val onClickListener: OnClickListener
) :
    RecyclerView.Adapter<BottomSheetChoiceAdapter.TestViewHolder>() {

    private var dataSource: List<BottomSheetDataModel> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setDataSource(listItem: List<BottomSheetDataModel>) {
        this.dataSource = listItem
        notifyDataSetChanged()
    }

    inner class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var userSelectionRadioButton: RadioButton? = null
        private var textViewDetail: TextView? = null

        init {
            userSelectionRadioButton = itemView.findViewById(R.id.userSelectionRadioButton)
            textViewDetail = itemView.findViewById(R.id.textViewDetail)
        }

        fun bind(model: BottomSheetDataModel) {
            userSelectionRadioButton?.text = model.itemName
            textViewDetail?.text = model.itemDetail
            userSelectionRadioButton?.setOnClickListener {
                onClickListener.onClick(it as RadioButton, layoutPosition)
            }
            userSelectionRadioButton?.isChecked = model.selected
        }


    }

    override fun getItemCount(): Int = dataSource.size

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val user = dataSource[position]
        holder.bind(user)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return TestViewHolder(view)

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
