package org.smartregister.fhircore.activity

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.adapter.AdverseEventItemRecyclerViewAdapter
import org.smartregister.fhircore.model.AdverseEventListItem

class AdverseEventListActivity : MultiLanguageBaseActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_adverse_event_list)

    //todo replace dummy data with the original data
    val list = listOf<AdverseEventListItem>(AdverseEventListItem("09-JAN-2021", "Blood clots"), AdverseEventListItem("10-JAN-2021", "Anaphylaxis"), AdverseEventListItem("11-JAN-2021", "Myocarditis"))
    val adapter = AdverseEventItemRecyclerViewAdapter(list)
    val recyclerView = findViewById<RecyclerView>(R.id.adverse_event_list)
    recyclerView.adapter = adapter
    adapter.notifyDataSetChanged()
  }
}