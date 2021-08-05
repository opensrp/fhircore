package org.smartregister.fhircore.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.smartregister.fhircore.R
import org.smartregister.fhircore.model.AdverseEventListItem

class AdverseEventItemRecyclerViewAdapter(private val adverseEventList: List<AdverseEventListItem>) : Adapter<AdverseEventItemRecyclerViewAdapter.ViewHolder>() {

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    lateinit var adverseEventListItem: AdverseEventListItem
    val date: TextView = view.findViewById(R.id.adverse_event_date)
    val resultingCondition: TextView = view.findViewById(R.id.adverse_event_detail)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adverse_event_item, parent, false))

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val adverseEventListItem = adverseEventList[position]
    holder.adverseEventListItem = adverseEventListItem
    holder.date.text = adverseEventListItem.date
    holder.resultingCondition.text = adverseEventListItem.resultingCondition
  }

  override fun getItemCount() = adverseEventList.size
}