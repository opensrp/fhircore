package org.smartregister.fhircore.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import org.smartregister.fhircore.R
import org.smartregister.fhircore.api.OauthService
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.account.OauthResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity: AppCompatActivity() {
    private lateinit var usernameTxt: EditText
    private lateinit var passwordTxt: EditText
    private lateinit var rememberChk: CheckBox
    private lateinit var forgotpwTvw: TextView
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        usernameTxt = findViewById(R.id.login_username_input)
        passwordTxt = findViewById(R.id.login_password_input)
        rememberChk = findViewById(R.id.login_remember_password_button)
        forgotpwTvw = findViewById(R.id.login_forgot_password_button)
        loginButton = findViewById(R.id.login_button)

        usernameTxt.doAfterTextChanged {
            evaluateLogin()
        }
        passwordTxt.doAfterTextChanged {
            evaluateLogin()
        }

        loginButton.setOnClickListener(View.OnClickListener {
            Toast.makeText(this, "I am clicked!!! yeah", Toast.LENGTH_LONG).show()
            AccountHelper().fetchToken(
                usernameTxt.text.toString(),
                passwordTxt.text.toString().toCharArray())
                .enqueue(object : Callback<OauthResponse>{
                    override fun onResponse(
                        call: Call<OauthResponse>,
                        response: Response<OauthResponse>
                    ) {
                        Log.i(localClassName, response.toString())
                    }

                    override fun onFailure(call: Call<OauthResponse>, t: Throwable) {
                        Log.e(localClassName, t.stackTraceToString())
                    }


                })
        })
    }

    override fun onStart() {
        super.onStart()

        evaluateLogin()
    }

    private fun evaluateLogin() {
        Log.i(this.localClassName, "evaluateLogin: ")

        val username: String = usernameTxt.text.toString().trim()
        val password: String = passwordTxt.text.toString().trim()
        val populated = username.isNotEmpty() && password.isNotEmpty()

        loginButton.isEnabled = populated
        if(populated){
            loginButton.setBackgroundResource(R.color.colorPrimaryLight)
        }
        else {
            loginButton.setBackgroundResource(R.color.light_gray)
        }
    }
}