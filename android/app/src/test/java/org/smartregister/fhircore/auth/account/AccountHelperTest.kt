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

package org.smartregister.fhircore.auth.account

import OAuthService
import io.mockk.every
import io.mockk.mockkObject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasEntry
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.smartregister.fhircore.BuildConfig
import org.smartregister.fhircore.api.OAuthService

class AccountHelperTest {
    private var mockOauthService: OAuthService? = null
    private lateinit var accountHelper: AccountHelper

    @Captor
    lateinit var captor: ArgumentCaptor<Map<String, String>>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this);

        mockOauthService = mock(OAuthService::class.java)

        mockkObject(OAuthService.Companion)

        every { OAuthService.create() } returns mockOauthService

        accountHelper = AccountHelper()
    }

    fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

    @Test
    fun `verify fetch token api`(){
        accountHelper.fetchToken("testuser", "testpass".toCharArray())

        verify(mockOauthService)?.fetchToken(capture(captor))

        assertThat(captor.value, hasEntry("client_id", BuildConfig.OAUTH_CIENT_ID))
        assertThat(captor.value, hasEntry("client_secret", BuildConfig.OAUTH_CLIENT_SECRET))
        assertThat(captor.value, hasEntry("grant_type", "password"))
        assertThat(captor.value, hasEntry("username", "testuser"))
        assertThat(captor.value, hasEntry("password", "testpass"))
    }

    @Test
    fun `verify refresh token api`(){
        //TODO var c: Call<OauthResponse>? = mock(Call::class.java) as Call<OauthResponse>?

       // `when`(mockOauthService?.fetchToken(any())?.execute()).thenReturn(c)

        runCatching {
            accountHelper.refreshToken("my test refresh token")

            verify(mockOauthService)?.fetchToken(capture(captor))

            assertThat(captor.value, hasEntry("client_id", BuildConfig.OAUTH_CIENT_ID))
            assertThat(captor.value, hasEntry("client_secret", BuildConfig.OAUTH_CLIENT_SECRET))
            assertThat(captor.value, hasEntry("grant_type", "refresh_token"))
            assertThat(captor.value, hasEntry("refresh_token", "my test refresh token"))
        }
    }
}