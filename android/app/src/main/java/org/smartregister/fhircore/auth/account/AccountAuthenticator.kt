package org.smartregister.fhircore.auth.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.core.os.bundleOf
import org.smartregister.fhircore.activity.LoginActivity


class AccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

    private var myContext = context;

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        TODO("Not yet implemented")
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        var am = AccountManager.get(myContext);

        var accessToken = am.peekAuthToken(account, authTokenType)
        var tokenResponse: OauthResponse

        // if no access token try getting one with refresh token
        if(accessToken.isNullOrEmpty()){
            var refreshToken = am.getPassword(account)

            if(!refreshToken.isNullOrEmpty()){
                tokenResponse = AccountHelper().refreshToken(refreshToken)!!

                accessToken = tokenResponse?.accessToken

                am.setPassword(account, refreshToken);
                am.setAuthToken(account, authTokenType, tokenResponse!!.accessToken);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.notifyAccountAuthenticated(account);
                }
            }
        }

        if (!accessToken.isNullOrEmpty()) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account!!.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account!!.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
            return result
        }

        return updateCredentials(response, account, authTokenType, options)
    }

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle {
        return Bundle()
    }
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle {
        return Bundle(options)
    }

    override fun getAuthTokenLabel(authTokenType: String?): String {
        TODO("Not yet implemented")
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        val intent = Intent(
            myContext,
            LoginActivity::class.java
        )
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account!!.type)
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account!!.name)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        return bundleOf(Pair(AccountManager.KEY_BOOLEAN_RESULT, false))
    }
}