package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.smartregister.fhircore.engine.R

abstract class RecyclerRegisterFragment<I : Any, O : Any> : BaseRegisterFragment<I, O>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.base_register_fragment, container, false)
}
