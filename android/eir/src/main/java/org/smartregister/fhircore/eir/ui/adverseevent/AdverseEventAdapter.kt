package org.smartregister.fhircore.eir.ui.adverseevent

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.ui.adverseevent.AdverseEventAdapter.*
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.ImmunizationAdverseEventItem

class AdverseEventAdapter :
  ListAdapter<ImmunizationAdverseEventItem, AdverseEventViewHolder>(AdverseEventItemDiffCallback) {

  inner class AdverseEventViewHolder(private val containerView: View) :
    RecyclerView.ViewHolder(containerView) {
      fun bindTo(immunizationAdverseEventItem: ImmunizationAdverseEventItem) {
      with(immunizationAdverseEventItem) {
        containerView.tag = this
        containerView.findViewById<TextView>(R.id.vaccineNameTextView).text = vaccine
        val vaccineAdverseEventsLayout = containerView.findViewById<LinearLayout>(R.id.vaccineAdverseEventLayout)
        addVaccineAdverseEventViews(vaccineAdverseEventsLayout, dosesWithAdverseEvents)
      }
      }

    private fun addVaccineAdverseEventViews(vaccineAdverseEventsLayout: LinearLayout, dosesWithAdverseEvents: List<Pair<String, List<AdverseEventItem>>>) {
      vaccineAdverseEventsLayout.removeAllViews()
      dosesWithAdverseEvents.forEach { dosesWithAdverseEvents ->
        val (vaccineName, adverseEvents) = dosesWithAdverseEvents
        val doseTextView =
          TextView(vaccineAdverseEventsLayout.context).apply {
            setTextColor(ContextCompat.getColor(vaccineAdverseEventsLayout.context, R.color.black))
            setPadding(0,0,0,16)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            text = vaccineName
          }
        vaccineAdverseEventsLayout.addView(doseTextView)

        val divider = View(vaccineAdverseEventsLayout.context).apply {
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, 1)
          setBackgroundColor(ContextCompat.getColor(vaccineAdverseEventsLayout.context, R.color.grey_drawable_color))
        }
        vaccineAdverseEventsLayout.addView(divider)

        dosesWithAdverseEvents.second.forEach { adverseEventReactionItem ->
          val adverseEventTextView =
            TextView(vaccineAdverseEventsLayout.context).apply {
              setTextColor(ContextCompat.getColor(vaccineAdverseEventsLayout.context, R.color.black))
              setPadding(0,0,0,16)
              setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

              text = adverseEventReactionItem.date + "   " + adverseEventReactionItem.detail
            }
          vaccineAdverseEventsLayout.addView(adverseEventTextView)
        }
        val space = View(vaccineAdverseEventsLayout.context).apply {
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, 16)
        }
        vaccineAdverseEventsLayout.addView(space)

      }
    }

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdverseEventViewHolder {
    val containerView = LayoutInflater.from(parent.context).inflate(R.layout.adverse_event_list_item, parent, false)
    return AdverseEventViewHolder(containerView)
  }

  override fun onBindViewHolder(holder: AdverseEventViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  object AdverseEventItemDiffCallback: DiffUtil.ItemCallback<ImmunizationAdverseEventItem>() {
    override fun areItemsTheSame(oldItem: ImmunizationAdverseEventItem, newItem: ImmunizationAdverseEventItem) =
      oldItem.vaccine == newItem.vaccine

    override fun areContentsTheSame(oldItem: ImmunizationAdverseEventItem, newItem: ImmunizationAdverseEventItem) =
      oldItem == newItem

  }

}