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

import android.os.Build
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.jetbrains.annotations.VisibleForTesting

fun CharArray.toPasswordHash(salt: ByteArray) = passwordHashString(this, salt)

@VisibleForTesting
fun passwordHashString(password: CharArray, salt: ByteArray): String {
  val pbKeySpec = PBEKeySpec(password, salt, 800000, 256)
  val secretKeyFactory =
    SecretKeyFactory.getInstance(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "PBKDF2withHmacSHA256"
      else "PBKDF2WithHmacSHA1"
    )
  return secretKeyFactory.generateSecret(pbKeySpec).encoded.toString(StandardCharsets.UTF_8)
}

fun Int.getRandomBytesOfSize(): ByteArray {
  val random = SecureRandom()
  val randomSaltBytes = ByteArray(this)
  random.nextBytes(randomSaltBytes)
  return randomSaltBytes
}

fun clearPasswordInMemory(charArray: CharArray) = Arrays.fill(charArray, '*')

fun CharArray.safePlus(element: Char): CharArray {
  val index = size
  val result = this.copyOf(index + 1)
  result[index] = element
  clearPasswordInMemory(this)
  return result
}

fun CharArray.safeRemoveLast(): CharArray {
  val index = size
  val result = this.copyOf(index - 1)
  clearPasswordInMemory(this)
  return result
}
