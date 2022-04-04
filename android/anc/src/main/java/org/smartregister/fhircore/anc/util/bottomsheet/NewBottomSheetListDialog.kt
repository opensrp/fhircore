package org.smartregister.fhircore.anc.util.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class NewBottomSheetListDialog(
    private val bottomSheetHolder: BottomSheetHolder,
    private val onBottomSheetListener: BottomSheetListDialog.OnClickedListItems
) : BottomSheetDialogFragment() {

    init {
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    BottomSheetListView(
                        bottomSheetHolder = bottomSheetHolder,
                        onBottomSheetListener = onBottomSheetListener
                    )
                }
            }
        }
    }
}
