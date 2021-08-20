package org.smartregister.fhircore.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_register_list.base_register_toolbar
import kotlinx.android.synthetic.main.toolbar_base_register.btn_show_overdue
import kotlinx.android.synthetic.main.toolbar_base_register.layout_scan_barcode
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.fragment.FamilyListFragment
import org.smartregister.fhircore.fragment.PAGE_COUNT
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.model.FamilyDetailView
import org.smartregister.fhircore.model.RegisterFamilyMemberData
import org.smartregister.fhircore.model.RegisterFamilyMemberResult
import org.smartregister.fhircore.model.RegisterFamilyResult
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.FamilyListViewModel
import org.smartregister.fhircore.viewmodel.FhirListViewModelFactory

class FamilyListActivity: BaseRegisterActivity() {
    lateinit var listViewModel: FamilyListViewModel
    private lateinit var familyRegistration: ActivityResultLauncher<FamilyDetailView>
    private lateinit var familyMemberRegistration: ActivityResultLauncher<RegisterFamilyMemberData>

    private lateinit var detailView: FamilyDetailView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setToolbarItemText(
            R.id.tv_clients_list_title,
            base_register_toolbar,
            R.string.client_list_title_family
        )

        setNavigationHeaderTitle(detailView.registerTitle, R.id.tv_nav_header)

        btn_show_overdue.visibility = View.INVISIBLE
        layout_scan_barcode.visibility = View.GONE

        listViewModel =
            ViewModelProvider(
                this,
                FhirListViewModelFactory(application, FhirApplication.fhirEngine(baseContext))
            )
                .get(FamilyListViewModel::class.java)

        familyRegistration =
            registerForActivityResult(RegisterFamilyResult()) {
                it?.run { handleRegisterFamilyResult(it) } // todo handle questionnaire failures
            }

        familyMemberRegistration =
            registerForActivityResult(RegisterFamilyMemberResult()) {
                it?.run { handleRegisterFamilyResult(it) } // todo handle questionnaire failures
            }

    }

    private fun handleRegisterFamilyResult(headId: String) {
        AlertDialog.Builder(this)
            .setMessage("Register another family member?")
            .setCancelable(false)
            .setNegativeButton("No") {dialogInterface, _ ->
                dialogInterface.dismiss()
                reloadList()
            }
            .setPositiveButton("Yes") { dialogInterface, _ ->
                dialogInterface.dismiss()
                familyMemberRegistration.launch(RegisterFamilyMemberData(headId, detailView))
            }.show()
    }

    override fun buildRegister(): BaseRegister {
        detailView =
            Utils.loadConfig(FamilyDetailView.FAMILY_DETAIL_VIEW_CONFIG_ID,
                FamilyDetailView::class.java,
                this)

        return BaseRegister(
            context = this,
            contentLayoutId = R.layout.activity_register_list,
            listFragment = FamilyListFragment(),
            viewPagerId = R.id.list_pager,
            newRegistrationViewId = R.id.btn_register_new_client,
            newRegistrationQuestionnaireIdentifier = detailView.registrationQuestionnaireIdentifier,
            newRegistrationQuestionnaireTitle = detailView.registrationQuestionnaireTitle,
            searchBoxId = R.id.edit_text_search
        )
    }

    override fun startRegistrationActivity(preAssignedId: String?) {
        familyRegistration.launch(detailView)
    }

    fun reloadList(){
        listViewModel.searchResults("", 0, PAGE_COUNT)
    }
}