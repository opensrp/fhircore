package org.smartregister.fhircore.engine.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttpClientQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OkHttpClientQualifier
