package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.engine.R

class BaseRegisterFragment : Fragment() {

  companion object {
    fun newInstance() = BaseRegisterFragment()
  }

  private lateinit var viewModel: RegisterViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.base_register_fragment, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)
  }
}
