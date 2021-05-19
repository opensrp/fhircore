package org.smartregister.fhircore.auth.account

import android.R.attr
import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import org.smartregister.fhircore.activity.LoginActivity
import org.smartregister.fhircore.auth.account.AccountConfig.AUTH_HANDLER_ACTIVITY
import timber.log.Timber


class AccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

    private val myContext = context;

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        Timber.i("Adding account of type $accountType with auth token of type $authTokenType")

        val intent = Intent(myContext, AUTH_HANDLER_ACTIVITY)

        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, attr.accountType)
        intent.putExtra(AccountConfig.KEY_AUTH_TOKEN_TYPE, authTokenType) //todo
        intent.putExtra(AccountConfig.KEY_IS_NEW_ACCOUNT, true)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        val am = AccountManager.get(myContext)

        var accessToken = am.peekAuthToken(account, authTokenType)
        var tokenResponse: OauthResponse

        // if no access token try getting one with refresh token
        if(accessToken.isNullOrEmpty()){
            val refreshToken = am.getPassword(account)

            Timber.i("GetAuthToken: AccessToken (%b), Refresh Token (%b) for %s ",
                accessToken.isNullOrEmpty(), refreshToken.isNullOrEmpty(), account?.name)

            if (!refreshToken.isNullOrEmpty()) {
                runCatching {
                    tokenResponse = AccountHelper().refreshToken(refreshToken)

                    accessToken = tokenResponse.accessToken

                    am.setPassword(account, refreshToken)
                    am.setAuthToken(account, authTokenType, accessToken)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.notifyAccountAuthenticated(account)
                    }
                }
                .onFailure {
                    Timber.i("Error refreshing token")
                    Timber.i(it.stackTraceToString())
                }
                .onSuccess {
                    Timber.i("Got new accessToken and registered")
                }

            }
        }

        if (!accessToken.isNullOrEmpty()) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
            return result
        }

        // failed to validate any token. now update credentials using auth activity
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
        return authTokenType?.toUpperCase().toString()
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        val intent = Intent(
            myContext,
            AUTH_HANDLER_ACTIVITY
        )
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account?.name)
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