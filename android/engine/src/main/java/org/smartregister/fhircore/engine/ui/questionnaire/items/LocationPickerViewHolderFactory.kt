package org.smartregister.fhircore.engine.ui.questionnaire.items

import android.view.View
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import org.smartregister.fhircore.engine.R

class LocationPickerViewHolderFactory(val customQuestItemDataProvider: CustomQuestItemDataProvider) :
    QuestionnaireItemViewHolderFactory(R.layout.custom_quest_location_picker_item) {

    override fun getQuestionnaireItemViewHolderDelegate(): QuestionnaireItemViewHolderDelegate =
        object : QuestionnaireItemViewHolderDelegate {

            private lateinit var locationPickerView: LocationPickerView
            override lateinit var questionnaireViewItem: QuestionnaireViewItem

            override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
            }

            override fun init(itemView: View) {
                locationPickerView = itemView.findViewById(R.id.location_picker_view)
                locationPickerView.setCustomDataProvider(customQuestItemDataProvider)
            }

            override fun setReadOnly(isReadOnly: Boolean) {
                locationPickerView.isEnabled = !isReadOnly
            }

        }

    companion object {
        const val WIDGET_EXTENSION = "https://d-tree.org/fhir/extensions/location-widget"
        const val WIDGET_TYPE = "location-widget"
    }
}