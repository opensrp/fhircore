/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import org.junit.Assert
import org.junit.jupiter.api.Test

internal class SecurityUtilKtTest {

  @Test
  fun testCharToPasswordHashGeneratesHash() {
    val secretPassword = "MySecretPassword"
    val hashedPassword =
      secretPassword.toCharArray().toPasswordHash(byteArrayOf(102, 103, 105, 107))
    Assert.assertNotNull(hashedPassword)
    Assert.assertNotEquals(secretPassword, hashedPassword)
  }

  @Test
  fun testPasswordHashStringGeneratesHash() {
    val secretPassword = "MySecretPassword"
    val hashedPassword =
      secretPassword.toCharArray().toPasswordHash(byteArrayOf(102, 103, 105, 107))
    Assert.assertNotNull(hashedPassword)
    Assert.assertNotEquals(secretPassword, hashedPassword)
  }

  @Test
  fun testGetRandomBytesOfSizeGeneratesRandomByteArray() {
    val firstBytes = 5.getRandomBytesOfSize()
    Assert.assertNotNull(firstBytes)
    Assert.assertEquals(5, firstBytes.size)

    val secondBytes = 5.getRandomBytesOfSize()
    Assert.assertNotNull(secondBytes)
    Assert.assertEquals(5, firstBytes.size)

    Assert.assertNotEquals(firstBytes, secondBytes)
  }

  @Test
  fun testClearPasswordInMemoryOverwritesContent() {
    val password = charArrayOf('1', '2', '0', '3', 'a', '5')
    clearPasswordInMemory(password)
    Assert.assertArrayEquals(charArrayOf('*', '*', '*', '*', '*', '*'), password)
  }

  @Test
  fun testCharSafePlus() {
    var originalArray = charArrayOf('1', '2', '3')
    val extendedArray = originalArray.safePlus('a')

    Assert.assertEquals(4, extendedArray.size)
    Assert.assertArrayEquals(charArrayOf('1', '2', '3', 'a'), extendedArray)
  }

  @Test
  fun testCharRemoveLast() {
    var originalArray = charArrayOf('1', '2', '3')
    val reductedArray = originalArray.safeRemoveLast()

    Assert.assertEquals(2, reductedArray.size)
    Assert.assertArrayEquals(charArrayOf('1', '2'), reductedArray)
  }
}
