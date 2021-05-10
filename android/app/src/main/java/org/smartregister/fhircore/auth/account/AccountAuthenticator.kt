package org.smartregister.fhircore.auth.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.os.Bundle

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

        var tokenResponse: OauthResponse =  null
        // if no access token try getting one with refresh token
        if(accessToken.isNullOrEmpty()){
            var refreshToken = am.getPassword(account)

            if(!refreshToken.isNullOrEmpty()){
                var tokenResponse = AccountHelper().refreshToken(refreshToken)

                am.setPassword(account, refreshToken);
                am.setAuthToken(account, authTokenType, tokenResponse?.accessToken);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.notifyAccountAuthenticated(account);
                }
            }
        }

        val toReturn = Bundle()
        toReturn.putString(AccountManager.KEY_AUTHTOKEN, tokenResponse?.accessToken)
        return toReturn
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
        TODO("Not yet implemented")
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        TODO("Not yet implemented")
    }
}