package org.smartregister.fhircore.anc.util.bottomsheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class NewBottomSheetListDialog(
    @NonNull context: Context,
    private val bottomSheetHolder: BottomSheetHolder,
    private val onBottomSheetListener: BottomSheetListDialog.OnClickedListItems
) : BottomSheetDialog(context) {

    init {
        ComposeView(context).apply {
            setContent {
                BottomSheetListView(
                    bottomSheetHolder = bottomSheetHolder,
                    onBottomSheetListener = onBottomSheetListener
                )
            }
        }
        setCancelable(false)
    }
}
