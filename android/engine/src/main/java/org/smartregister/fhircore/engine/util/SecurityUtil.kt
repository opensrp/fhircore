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

import java.security.MessageDigest
import java.util.Locale
import javax.xml.bind.DatatypeConverter

fun String.toSha1() = hashString("SHA-1", this)

private fun hashString(type: String, input: String): String {
  val bytes = MessageDigest.getInstance(type).digest(input.toByteArray())
  return DatatypeConverter.printHexBinary(bytes).uppercase(Locale.getDefault())
}
