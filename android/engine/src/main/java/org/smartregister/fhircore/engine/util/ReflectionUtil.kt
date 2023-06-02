package org.smartregister.fhircore.engine.util

import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

fun <T : Any> KClass<T>.callFunction(method: String, vararg args: Any) = this.declaredMemberFunctions
.firstOrNull { it.name == method }
?.apply { isAccessible = true }
?.call(this, args)

suspend fun <T : Any> KClass<T>.callSuspendFunctionOnField(obj: T, field: String, method: String, vararg args: Any): Any? {
    val declaredObject = this.declaredMemberProperties.first { it.name == field }.apply { this.isAccessible = true }.get(obj)!!
    val declaredObjectMethod = declaredObject::class.declaredMemberFunctions
        .first { it.name == method }
        .apply { isAccessible = true }
    return declaredObjectMethod.callSuspend(declaredObject, *args)
}