package org.smartregister.fhircore.viewmodel

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.databinding.InverseMethod
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.material.internal.TextWatcherAdapter
import okhttp3.ResponseBody
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientListActivity
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.OAuthResponse
import org.smartregister.fhircore.auth.secure.Credentials
import org.smartregister.fhircore.auth.secure.SecureConfig
import org.smartregister.fhircore.model.LoginUser
import org.smartregister.fhircore.util.SharedPrefrencesHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class LoginViewModel(application: Application) : AndroidViewModel(application), Callback<OAuthResponse> {

  var loginUser: LoginUser = LoginUser()
  var allowLogin = MutableLiveData(false)
  var loginFailed = MutableLiveData(false)
  var showPassword= MutableLiveData(false)

  private val secureConfig: SecureConfig = SecureConfig(application)
  private var baseContext: Context = application.baseContext

  private var accountManager: AccountManager

  init {
    Timber.i("Starting auth flow for login")

    if (BuildConfig.DEBUG && BuildConfig.SKIP_AUTH_CHECK) {
      startHome()
    }

    accountManager = AccountManager.get(application)

    AccountHelper(baseContext).loadAccount(
      accountManager,
      secureConfig.retrieveSessionUsername(),
      OnTokenAcquired(),
      Handler(OnTokenError())
    )
  }

  private fun shouldEnableLogin(): Boolean {
    val username: String = loginUser.username
    val password: CharArray = loginUser.password

    return username.isNotEmpty() && password.isNotEmpty()
  }

  private fun enableLoginButton(enable: Boolean) {
    allowLogin.value = enable
  }

  var credentialsWatcher: TextWatcher =
    object : TextWatcherAdapter() {
      override fun afterTextChanged(s: Editable) {
        enableLoginButton(shouldEnableLogin())
      }
    }

  fun remoteLogin(view: View) {
    Timber.i("Logging in for ${loginUser.username} ")

    enableLoginButton(false)

    AccountHelper(baseContext).fetchToken(loginUser.username, loginUser.password).enqueue(this)
  }

  override fun onResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
    Timber.i("Got login response ${response.isSuccessful}")

    loginFailed.value = !response.isSuccessful

    if (!response.isSuccessful) {
      Timber.i("Error in fetching credentials %s", response.errorBody())

      enableLoginButton(true)

      return
    }

    AccountHelper(baseContext).addAuthenticatedAccount(accountManager, response, loginUser.username)

    val accessToken = response.body()!!.accessToken!!

    secureConfig.saveCredentials(Credentials(loginUser.username, loginUser.password, accessToken))

    // destroy password
    loginUser.password = charArrayOf()

    AccountHelper(baseContext)
      .getUserInfo()
      .enqueue(OnUserInfoResponse(baseContext))

    startHome()
  }

  override fun onFailure(call: Call<OAuthResponse>, t: Throwable) {
    Timber.e(javaClass.name, t.stackTraceToString())

    Toast.makeText(baseContext, R.string.login_call_fail_error_message, Toast.LENGTH_LONG).show()

    if (allowLocalLogin()) {
      startHome()
      return
    }

    enableLoginButton(true)
  }

  private fun startHome() {
    val intent = Intent(getApplication(), PatientListActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    baseContext.startActivity(intent)
  }

  private fun allowLocalLogin(): Boolean {
    val creds = secureConfig.retrieveCredentials()
    if (creds?.username.equals(loginUser.username) &&
        creds?.password?.concatToString().equals(loginUser.password.concatToString())
    ) {
      return true
    }
    return false
  }

  object Converter {
    @InverseMethod("stringToCharArray")
    @JvmStatic
    fun charArrayToString(value: CharArray): String {
      return value.concatToString()
    }

    @JvmStatic
    fun stringToCharArray(value: String): CharArray {
      return value.toCharArray()
    }
  }
}

private class OnUserInfoResponse (context: Context): Callback<ResponseBody> {
  val baseContext = context;

  override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
    Timber.e(t)

    Toast.makeText(baseContext, R.string.userinfo_call_fail_error_message, Toast.LENGTH_LONG).show()
  }

  override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
    Timber.i(response?.body()?.string())

    SharedPrefrencesHelper.init(baseContext).write("USER", response?.body()?.string()!!)
  }
}

private class OnTokenError : Handler.Callback {
  override fun handleMessage(msg: Message): Boolean {
    Timber.i("Error ${msg.data}")

    return true // todo ??
  }
}

private class OnTokenAcquired : AccountManagerCallback<Bundle> {

  override fun run(result: AccountManagerFuture<Bundle>) {
    // Get the result of the operation from the AccountManagerFuture.
    val bundle: Bundle = result.result

    // The token is a named value in the bundle. The name of the value
    // is stored in the constant AccountManager.KEY_AUTHTOKEN.
    val token: String? = bundle.getString(AccountManager.KEY_AUTHTOKEN)
    val resp: AccountAuthenticatorResponse? =
      bundle.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    val launch: Intent? = bundle.get(AccountManager.KEY_INTENT) as? Intent

    Timber.i("Got launch ${launch?.data}")

    Timber.i("Got token $token")

    Timber.i("Got resp ${resp.toString()}")
  }
}
