/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.eir.viewmodel

import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.smartregister.fhircore.eir.FhirApplication
import org.smartregister.fhircore.eir.RobolectricTest
import org.smartregister.fhircore.eir.api.OAuthService
import org.smartregister.fhircore.eir.auth.OAuthResponse
import org.smartregister.fhircore.eir.auth.account.AccountHelper
import org.smartregister.fhircore.eir.auth.secure.Credentials
import org.smartregister.fhircore.eir.auth.secure.FakeKeyStore
import retrofit2.Call
import retrofit2.Response

class LoginViewModelTest : RobolectricTest() {

  private lateinit var viewModel: LoginViewModel

  @Before
  fun setUp() {
    viewModel = LoginViewModel(FhirApplication.getContext())
  }

  @Test
  fun testShouldEnableLoginShouldReturnTrue() {
    setUsernameAndPassword()
    Assert.assertTrue(viewModel.shouldEnableLogin())
  }

  @Test
  fun testAllowLoginShouldBeTrue() {

    setUsernameAndPassword()

    viewModel.credentialsWatcher.afterTextChanged(mockk())
    Assert.assertTrue(viewModel.allowLogin.value!!)
  }

  @Test
  fun testOnFailureShouldVerifyInternalCalls() {
    val call = mockk<Call<OAuthResponse>>()
    val t = Exception("Some sample message")

    setUsernameAndPassword()
    viewModel.secureConfig.saveCredentials(Credentials("testuser", charArrayOf('a'), "dummy_token"))
    viewModel.goHome.value = false
    viewModel.allowLogin.value = false
    viewModel.onFailure(call, t)

    Assert.assertTrue(viewModel.goHome.value!!)

    viewModel.secureConfig.deleteCredentials()
    viewModel.onFailure(call, t)

    Assert.assertTrue(viewModel.allowLogin.value!!)
  }

  @Test
  fun testRemoteLoginShouldVerifyInternalCalls() {
    val accountManagerSpy = spyk(viewModel.accountManager)
    val secureConfigSpy = spyk(viewModel.secureConfig)
    val accountHelperSpy = spyk(viewModel.accountHelper)

    viewModel.accountHelper = accountHelperSpy
    viewModel.accountManager = accountManagerSpy
    viewModel.secureConfig = secureConfigSpy

    mockkObject(OAuthService)

    val responseCall = mockk<Call<OAuthResponse>>()
    val response =
      Response.success(
        OAuthResponse(accessToken = "829382938", refreshToken = "32893239", expiresIn = 600)
      )

    every { responseCall.execute() } returns response
    every { responseCall.enqueue(any()) } answers { viewModel.onResponse(responseCall, response) }
    every { OAuthService.create(any()).fetchToken(any()) } returns responseCall
    every { OAuthService.create(any()).userInfo().enqueue(any()) } returns mockk()
    every { accountManagerSpy.notifyAccountAuthenticated(any()) } returns true

    setUsernameAndPassword()
    viewModel.remoteLogin(mockk())

    verify(exactly = 1) { accountHelperSpy.fetchToken(any(), any()) }
    verify(exactly = 1) { accountHelperSpy.addAuthenticatedAccount(any(), any(), any()) }
    verify(exactly = 1) { secureConfigSpy.saveCredentials(any()) }
    verify(exactly = 1) { accountHelperSpy.getUserInfo() }

    unmockkObject(OAuthService)
  }

  @Test
  fun testRunShouldSetGotoHomeValueTrue() {
    val future = mockk<AccountManagerFuture<Bundle>>()
    val bundle = Bundle()

    bundle.putString(AccountManager.KEY_AUTHTOKEN, "dummy_authtoken")
    viewModel.goHome.value = false

    val field = viewModel.javaClass.getDeclaredField("accountHelper")
    field.isAccessible = true
    val accountHelper = spyk(field.get(viewModel) as AccountHelper)
    field.set(viewModel, accountHelper)

    every { accountHelper.isSessionActive(any()) } returns true
    every {
      hint(Bundle::class)
      future.result
    } returns bundle

    viewModel.run(future)

    Assert.assertTrue(viewModel.goHome.value!!)
  }

  private fun setUsernameAndPassword() {
    viewModel.loginUser.username = "testuser"
    viewModel.loginUser.password = charArrayOf('a')
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
