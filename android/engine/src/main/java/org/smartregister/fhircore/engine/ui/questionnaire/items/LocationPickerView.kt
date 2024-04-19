package org.smartregister.fhircore.engine.ui.questionnaire.items

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import org.smartregister.fhircore.engine.R

class LocationPickerView(
    context: Context,
    attrs: AttributeSet,
) : androidx.appcompat.widget.AppCompatButton(context, attrs) {
    private val items = arrayOf("Item 1", "Item 2", "Item 3")
    private var customQuestItemDataProvider: CustomQuestItemDataProvider? = null

    init {
        this.setOnClickListener {
            showDropdownDialog()
        }
    }

    private fun showDropdownDialog() {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.custom_location_picker_view, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)

        val mainSpinner = dialogView.findViewById<Spinner>(R.id.main_location_picker)
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items)
        mainSpinner.setAdapter(adapter)

        val dialog = builder.create()
        dialog.show()
    }

    private fun initData() {
        customQuestItemDataProvider?.let {
            var location = it.fetchLocationHierarchies().firstOrNull()
        }
    }

    fun setCustomDataProvider(customQuestItemDataProvider: CustomQuestItemDataProvider) {
        this.customQuestItemDataProvider = customQuestItemDataProvider
        initData()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}