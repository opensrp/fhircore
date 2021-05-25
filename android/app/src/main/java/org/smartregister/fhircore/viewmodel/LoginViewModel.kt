package org.smartregister.fhircore.viewmodel

import android.accounts.*
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import timber.log.Timber


class LoginViewModel (application: Application) : AndroidViewModel(application), Callback<OauthResponse>{
    var loginUser: LoginUser = LoginUser()
    var allowLogin = MutableLiveData(false)
    var loginFailed = MutableLiveData(false)

    val secureConfig: SecureConfig = SecureConfig(application)

    private lateinit var context: Context
    private var accountManager: AccountManager

    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private val mResultBundle: Bundle? = null

    init{
        loadCredentials()

        accountManager = AccountManager.get(application)

        val account = Account(
            loginUser.username,
            AccountConfig.ACCOUNT_TYPE)

        val accounts = AccountManager.get(application).getAccountsByType(AccountConfig.ACCOUNT_TYPE)

        Timber.i("Got account %s : ", accounts.get(0).name)

        val options = Bundle()

        var tok = accountManager.peekAuthToken(account, AccountConfig.AUTH_TOKEN_TYPE)

        Timber.i("PEEKED 1 $tok")

        tok = accountManager.peekAuthToken(account, AccountConfig.ACCOUNT_TYPE)

        Timber.i("PEEKED 2 $tok")

        var future = accountManager.getAuthToken(
            account,                     // Account retrieved using getAccountsByType()
            AccountConfig.ACCOUNT_TYPE, // ,            // Auth scope
            options,                        // Authenticator-specific options
            null,                           // Your activity
            OnTokenAcquired(),              // Callback called when a token is successfully acquired
            Handler(OnError())              // Callback called if an error occurs
        )

        Timber.i("FUTURE is done %b", future.isDone)
    }

    private fun loadCredentials() {
        Timber.i("Loading credentials from secure storage")

        val savedCreds = secureConfig.retrieveCredentials()

        if(savedCreds != null){
            // todo
            Timber.i("Found credentials for ${savedCreds.username} -- ${savedCreds.password}")

            loginUser.username = savedCreds.username
            loginUser.password = savedCreds.password
            loginUser.rememberMe = true

            enableLoginButton( shouldEnableLogin() )
        }
    }

    private fun shouldEnableLogin(): Boolean {
        val username: String = loginUser.username
        val password: CharArray = loginUser.password

        return username.isNotEmpty() && password.isNotEmpty()
    }

    private fun enableLoginButton(enable: Boolean) {
        allowLogin.value = enable
    }

    var credentialsWatcher: TextWatcher = object : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable) {
            enableLoginButton( shouldEnableLogin() )
        }
    }

    fun remoteLogin(view: View) {
        Timber.i("Logging in for ${loginUser.username} ")

        context = view.context

        enableLoginButton(false)

        AccountHelper().fetchToken(
                loginUser.username,
                loginUser.password)
            .enqueue(this)
    }

    override fun onResponse(call: Call<OauthResponse>, response: Response<OauthResponse>) {
        Timber.i(response.toString()) //todo

        loginFailed.value = !response.isSuccessful

        if(!response.isSuccessful){
            Timber.i("Error in fetching credentials %s", response.errorBody())

            enableLoginButton(true)

            return
        }

        val account = Account(
            loginUser.username,
            AccountConfig.ACCOUNT_TYPE)

        accountManager.addAccountExplicitly(account, response.body()?.refreshToken, null)
        accountManager.setAuthToken(account, AccountConfig.AUTH_TOKEN_TYPE, response.body()!!.accessToken)
        accountManager.setPassword(account, response.body()!!.refreshToken)
        /*mAccountManager.setUserData(
            account,
            AccountHelper.INTENT_KEY.ACCOUNT_LOCAL_PASSWORD,
            userData.getString(AccountHelper.INTENT_KEY.ACCOUNT_LOCAL_PASSWORD)
        )*/
        /*mAccountManager.setUserData(
            account,
            AccountHelper.INTENT_KEY.ACCOUNT_LOCAL_PASSWORD_SALT,
            userData.getString(AccountHelper.INTENT_KEY.ACCOUNT_LOCAL_PASSWORD_SALT)
        )*/
        /*mAccountManager.setUserData(
            account,
            AccountHelper.INTENT_KEY.ACCOUNT_NAME,
            userData.getString(AccountHelper.INTENT_KEY.ACCOUNT_NAME)
        )*/
//        mAccountManager.setUserData(
//            account,
//            AccountHelper.INTENT_KEY.ACCOUNT_REFRESH_TOKEN,
//            response.getRefreshToken()
//        )
        /*mAccountManager.setUserData(
            account,
            AccountHelper.INTENT_KEY.ACCOUNT_ROLES,
            if (user.getRoles() != null) user.getRoles()
                .toString() else Collections.EMPTY_LIST.toString()
        )*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accountManager.notifyAccountAuthenticated(account)
        }

        if(loginUser.rememberMe){
            Timber.i("Remember password for ${loginUser.username}")

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

        loginFailed.value = true

        enableLoginButton(true)
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

class OnError: Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        Timber.i("Error ${msg.data}")

        return true //todo ??
    }
}

private class OnTokenAcquired : AccountManagerCallback<Bundle> {

    override fun run(result: AccountManagerFuture<Bundle>) {
        // Get the result of the operation from the AccountManagerFuture.
        val bundle: Bundle = result.getResult()

        // The token is a named value in the bundle. The name of the value
        // is stored in the constant AccountManager.KEY_AUTHTOKEN.
        val token: String? = bundle.getString(AccountManager.KEY_AUTHTOKEN)
        val resp: AccountAuthenticatorResponse? = bundle.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        val launch: Intent? = bundle.get(AccountManager.KEY_INTENT) as? Intent

        Timber.i("Got launch ${launch?.data}")

        Timber.i("Got token $token")

        Timber.i("Got resp ${resp.toString()}")
    }
}
