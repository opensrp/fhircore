package org.smartregister.fhircore.engine.ui.questionnaire

import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.view.children
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewHolder
import com.google.android.fhir.datacapture.views.QuestionnaireItemViewItem
import com.google.android.material.textview.MaterialTextView
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.engine.R
import org.w3c.dom.Text

class QuestionnaireFragmentExtended() : QuestionnaireFragment() {
  private lateinit var recyclerView: RecyclerView
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recyclerView =
      requireActivity()
        .findViewById(com.google.android.fhir.datacapture.R.id.recycler_view)

    recyclerView.viewTreeObserver.addOnGlobalLayoutListener(
      object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          // At this point the layout is complete and the
          // dimensions of recyclerView and any child views
          // are known.
          val adapter =
            recyclerView.adapter!! as
              ListAdapter<QuestionnaireItemViewItem, QuestionnaireItemViewHolder>
          for (i in 0 until adapter.itemCount) {
            val viewHolder =
              recyclerView.findViewHolderForAdapterPosition(i) as QuestionnaireItemViewHolder
            val itemView = viewHolder?.itemView
            val itemQuestion = adapter.currentList[i]

            itemQuestion.questionnaireItem.handleRenderingStyleExtension(itemView)
          }

          recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      }
    )
  }

  private val HTML_RENDERING_XHTML_EXTENSION: String =
    "http://hl7.org/fhir/StructureDefinition/rendering-xhtml"

      private fun Questionnaire.QuestionnaireItemComponent.handleRenderingStyleExtension(itemView: View) {
          textElement?.extension
              ?.firstOrNull { it.url == HTML_RENDERING_XHTML_EXTENSION }
              ?.let { ext ->
                  // a question item in widget has two text views for text display
                  // 1- Prefix and 2- Text
                  // set both of these with given style incase visible and non empty
                  (itemView as ViewGroup).children
                      .take(2)
                      ?.forEach {
                          (it as TextView).text = HtmlCompat.fromHtml("${ext.value}", FROM_HTML_MODE_COMPACT)
                      }
              }
      }
}
