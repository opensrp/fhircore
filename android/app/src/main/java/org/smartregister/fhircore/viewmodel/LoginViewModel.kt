package org.smartregister.fhircore.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.databinding.InverseMethod
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.material.internal.TextWatcherAdapter
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.auth.account.*
import org.smartregister.fhircore.model.LoginUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginViewModel (application: Application) : AndroidViewModel(application), Callback<OauthResponse>{
    var loginUser: LoginUser = LoginUser()
    var allowLogin = MutableLiveData(false)

    val secureConfig: SecureConfig = SecureConfig(application)

    lateinit var context: Context

    init{
        loadCredentials()
    }

    private fun loadCredentials() {
        Log.i(javaClass.name, "Loading credentials from secure storage")

        var savedCreds = secureConfig.retrieveCredentials()

        if(savedCreds != null){
            // todo
            Log.i(javaClass.name, "Found credentials for ${savedCreds.username} -- ${savedCreds.password}")

            loginUser.username = savedCreds.username
            loginUser.password = savedCreds.password
            loginUser.rememberMe = true
            allowLogin.value = shouldEnableLogin()
        }
    }

    private fun shouldEnableLogin(): Boolean {
        val username: String = loginUser.username
        val password: CharArray = loginUser.password

        return username.isNotEmpty() && password.isNotEmpty()
    }

    var credentialsWatcher: TextWatcher = object : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable) {
            allowLogin.value = shouldEnableLogin()
        }
    }

    fun remoteLogin(view: View) {
        Log.i(javaClass.name, "Logging in for ${loginUser.username} ")

        AccountHelper().fetchToken(
                loginUser.username,
                loginUser.password)
            .enqueue(this)

        context = view.context
    }

    override fun onResponse(call: Call<OauthResponse>, response: Response<OauthResponse>) {
        Log.i(javaClass.name, response.toString()) //todo

        val account = Account(
            loginUser.username,
            AccountConfig.ACCOUNT_TYPE)
        AccountManager.get(getApplication()).addAccountExplicitly(account, response.body()!!.refreshToken, null)
        AccountManager.get(getApplication()).setAuthToken(account, AccountConfig.AUTH_TOKEN_TYPE, response.body()!!.accessToken)

        if(loginUser.rememberMe){
            Log.i(javaClass.name, "Remember password for ${loginUser.username}")

            secureConfig.saveCredentials(Credentials(loginUser.username, loginUser.password))
        }
        else {
            secureConfig.deleteCredentials()
        }

        // destroy password
        loginUser.password = charArrayOf()

        startHome()
    }

    private  fun startHome(){//todo find right way of calling setting context
        var intent = Intent(getApplication(), PatientListActivity::class.java)
        context.startActivity(intent);
    }

    override fun onFailure(call: Call<OauthResponse>, t: Throwable) {
        Log.e(javaClass.name, t.stackTraceToString())
    }

    object Converter {
        @InverseMethod("stringToCharArray")
        @JvmStatic fun charArrayToString(value: CharArray): String {
            return value.concatToString()
        }

        @JvmStatic fun stringToCharArray(value: String): CharArray {
            return value.toCharArray()
        }
    }
}