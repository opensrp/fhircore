package org.smartregister.fhircore.quest.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.configuration.geowidget.SummaryBottomSheetConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class SummaryBottomSheetFragment(
    private val summaryBottomSheetConfig: SummaryBottomSheetConfig,
    private val resourceData: ResourceData
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    SummaryBottomSheetView(
                        properties = summaryBottomSheetConfig.views!!,
                        resourceData = resourceData,
                        navController = findNavController()
                    )

                }
            }
        }
    }
    companion object {
        const val TAG = "SummaryBottomSheetTag"
    }

}