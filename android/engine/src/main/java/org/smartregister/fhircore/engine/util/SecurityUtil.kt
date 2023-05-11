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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.xml.bind.DatatypeConverter

fun String.toSha1() = hashString("SHA-1", this)

private fun hashString(type: String, input: String): String {
  val bytes = MessageDigest.getInstance(type).digest(input.toByteArray())
  return DatatypeConverter.printHexBinary(bytes).uppercase(Locale.getDefault())
}

fun CharArray.toPasswordHash(salt: ByteArray) = passwordHashString(this, salt)

fun passwordHashString(password: CharArray, salt: ByteArray): String {
  val pbKeySpec = PBEKeySpec(password, salt, 200000, 256)
  val secretKeyFactory =
    SecretKeyFactory.getInstance(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "PBKDF2withHmacSHA256"
      else "PBKDF2WithHmacSHA1"
    )
  return secretKeyFactory.generateSecret(pbKeySpec).encoded.toString(StandardCharsets.UTF_8)
}

fun getRandomBytesOfSize(size: Int): ByteArray {
  val random = SecureRandom()
  val randomSaltBytes = ByteArray(size)
  random.nextBytes(randomSaltBytes)
  return randomSaltBytes
}
