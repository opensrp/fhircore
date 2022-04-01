package org.smartregister.fhircore.anc.util.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class NewBottomSheetListDialog(private val itemListener: (String) -> Unit) : BottomSheetDialogFragment()  {

    lateinit var dataHolder: BottomSheetHolder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme { BottomSheetListView(bottomSheetHolder = dataHolder, itemListener = itemListener) }
            }
        }
    }
}