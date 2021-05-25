package org.smartregister.fhircore.activity

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.BR
import org.smartregister.fhircore.R
import org.smartregister.fhircore.databinding.LoginBinding
import org.smartregister.fhircore.viewmodel.LoginViewModel
import timber.log.Timber

class LoginActivity: AppCompatActivity() {

    lateinit var binding: LoginBinding
    lateinit var viewModel: LoginViewModel

    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        binding = DataBindingUtil.setContentView(this, R.layout.login)
        binding.setVariable(BR.usr, viewModel)
        binding.lifecycleOwner = this

        mAccountAuthenticatorResponse =
            intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)

        Timber.i("mAccountAuthenticatorResponse = %s", mAccountAuthenticatorResponse?.toString())
        mAccountAuthenticatorResponse?.onRequestContinued()
    }

    fun setAccountAuthenticatorResult(result: Bundle) {
        mResultBundle = result
    }

    override fun finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse!!.onResult(mResultBundle)
            } else {
                mAccountAuthenticatorResponse!!.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            mAccountAuthenticatorResponse = null
        }
        super.finish()
    }



}