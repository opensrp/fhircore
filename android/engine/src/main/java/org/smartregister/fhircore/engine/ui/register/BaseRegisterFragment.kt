package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.util.ListenerIntent

abstract class BaseRegisterFragment<I : Any, O : Any> : Fragment() {

  abstract val paginatedDataSource: PaginatedDataSource<I, O>

  abstract val registerDataViewModel: BaseRegisterDataViewModel<I, O>

  protected val registerViewModel by activityViewModels<RegisterViewModel>()

  /**
   * Implement functionality to navigate to details view when an item with [uniqueIdentifier] is
   * clicked
   */
  abstract fun navigateToDetails(uniqueIdentifier: String)

  /**
   * Implement click listener for list row. [listenerIntent] describe the intention of the current
   * click e.g. OPEN_PROFILE, EXIT etc so you can differentiate different click actions performed on
   * the UI elements e.g clicking a button to record vaccine or patient name to open their profile .
   * [data] of type [O] is also passed when item is clicked.
   */
  abstract fun onItemClicked(listenerIntent: ListenerIntent, data: O)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (requireActivity() !is BaseRegisterActivity) {
      throw (IllegalAccessException(
        "You can only use BaseRegisterFragment in BaseRegisterActivity context"
      ))
    }
  }
}
