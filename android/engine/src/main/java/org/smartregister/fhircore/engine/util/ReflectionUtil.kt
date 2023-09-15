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

import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

fun <T : Any> KClass<T>.callFunction(method: String, vararg args: Any) =
  this.declaredMemberFunctions
    .firstOrNull { it.name == method }
    ?.apply { isAccessible = true }
    ?.call(this, args)

suspend fun <T : Any> KClass<T>.callSuspendFunctionOnField(
  obj: T,
  field: String,
  method: String,
  vararg args: Any,
): Any? {
  val declaredObject =
    this.declaredMemberProperties
      .first { it.name == field }
      .apply { this.isAccessible = true }
      .get(obj)!!
  val declaredObjectMethod =
    declaredObject::class
      .declaredMemberFunctions
      .first { it.name == method }
      .apply { isAccessible = true }
  return declaredObjectMethod.callSuspend(declaredObject, *args)
}

inline fun <reified T : Any> getPrivateProperty(property: String, obj: T): Any? {
  return T::class
    .declaredMemberProperties
    .find { it.name == property }!!
    .apply { isAccessible = true }
    .get(obj)
}

fun <T : Any> Any.callSuperPrivateMember(method: String, vararg args: Any?): T {
  return this::class
    .superclasses
    .first()
    .declaredFunctions
    .find { it.name == method }!!
    .apply { isAccessible = true }
    .also { it }
    .call(this, *args) as T
}
