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

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.RobolectricTest

class AccountServiceTest : RobolectricTest() {

  private lateinit var accountService: AccountService

  @Before
  fun setUp() {
    accountService = Robolectric.buildService(AccountService::class.java, null).create().get()
  }

  @Test
  fun testOnCreateShouldCreateAccountAuthenticatorObject() {
    val field = accountService.javaClass.getDeclaredField("authenticator")
    field.isAccessible = true

    Assert.assertNotNull(field.get(accountService))
  }

  @Test
  fun testOnBindShouldReturnNonNullIBinder() {
    Assert.assertNotNull(accountService.onBind(null))
  }
}
