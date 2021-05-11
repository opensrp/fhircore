package org.smartregister.fhircore.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.material.internal.TextWatcherAdapter
import kotlinx.android.synthetic.main.login.*
import org.smartregister.fhircore.auth.account.AccountConfig
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.account.OauthResponse
import org.smartregister.fhircore.model.LoginUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginViewModel (application: Application) : AndroidViewModel(application), Callback<OauthResponse>{
    var loginUser: LoginUser = LoginUser()
    var allowLogin = MutableLiveData(false)

    private fun shouldEnableLogin(): Boolean {
        Log.i(javaClass.name, loginUser.username+" --- " + loginUser.password)

        val username: String = loginUser.username
        val password: String = loginUser.password

        return username.isNotEmpty() && password.isNotEmpty()
    }

    var credentialsWatcher: TextWatcher = object : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable) {
            allowLogin.value = shouldEnableLogin()

            Log.i(javaClass.name, "allowed "+allowLogin.value)
        }
    }

    fun remoteLogin(view: View) {
        AccountHelper().fetchToken(
                loginUser.username,
                loginUser.password.toCharArray())
            .enqueue(this)
    }

    override fun onResponse(call: Call<OauthResponse>, response: Response<OauthResponse>) {
        Log.i(javaClass.name, response.toString())

        val account = Account(
            loginUser.username,
            AccountConfig.ACCOUNT_TYPE)
        AccountManager.get(getApplication()).addAccountExplicitly(account, response.body()!!.refreshToken, null)
        AccountManager.get(getApplication()).setAuthToken(account, AccountConfig.AUTH_TOKEN_TYPE, response.body()!!.accessToken)

    }

    override fun onFailure(call: Call<OauthResponse>, t: Throwable) {
        Log.e(javaClass.name, t.stackTraceToString())
    }

}