package org.smartregister.fhircore.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.BR
import org.smartregister.fhircore.R
import org.smartregister.fhircore.databinding.LoginBinding
import org.smartregister.fhircore.viewmodel.LoginViewModel

class LoginActivity: AppCompatActivity() {

    lateinit var binding: LoginBinding
    lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        binding = DataBindingUtil.setContentView(this, R.layout.login)
        binding.setVariable(BR.usr, viewModel)
        binding.lifecycleOwner = this
    }
}