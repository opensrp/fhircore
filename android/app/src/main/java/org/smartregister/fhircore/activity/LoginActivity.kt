package org.smartregister.fhircore.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.login.*
import org.smartregister.fhircore.BR
import org.smartregister.fhircore.R
import org.smartregister.fhircore.auth.account.AccountConfig
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.account.OauthResponse
import org.smartregister.fhircore.databinding.LoginBinding
import org.smartregister.fhircore.viewmodel.LoginViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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