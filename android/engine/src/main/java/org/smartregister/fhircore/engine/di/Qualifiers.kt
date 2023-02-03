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

package org.smartregister.fhircore.engine.di

import javax.inject.Qualifier
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@ExcludeFromJacocoGeneratedReport
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthorizationOkHttpClientQualifier

@ExcludeFromJacocoGeneratedReport
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedOkHttpClientQualifier

@ExcludeFromJacocoGeneratedReport
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticationRetrofit

@ExcludeFromJacocoGeneratedReport
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KeycloakRetrofit

@ExcludeFromJacocoGeneratedReport
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularRetrofit
