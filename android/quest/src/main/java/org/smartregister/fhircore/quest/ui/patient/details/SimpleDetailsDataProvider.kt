package org.smartregister.fhircore.quest.ui.patient.details

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem

interface SimpleDetailsDataProvider {
    val detailsViewItem: MutableLiveData<DetailsViewItem>

    val onBackPressClicked: MutableLiveData<Boolean>
        get() = MutableLiveData(false)

    val onMenuItemClicked: MutableLiveData<Int>
        get() = MutableLiveData(-1)

    fun onMenuItemClickListener(@StringRes id: Int) {
        onMenuItemClicked.value = id
    }

    fun onBackPressed(backPressed: Boolean) {
        onBackPressClicked.value = backPressed
    }
}