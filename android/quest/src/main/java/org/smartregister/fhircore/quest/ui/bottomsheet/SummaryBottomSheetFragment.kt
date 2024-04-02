package org.smartregister.fhircore.quest.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class SummaryBottomSheetFragment(
    val navigationMenuConfigs: List<NavigationMenuConfig>? = emptyList(),
    val title: String? = null,
    val menuClickListener: (NavigationMenuConfig) -> Unit,
) : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    BottomSheetContent(
                        listImageProperties = arrayListOf(ImageProperties()),
                        navController = findNavController()
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "NavigationBottomSheetTag"
    }

}