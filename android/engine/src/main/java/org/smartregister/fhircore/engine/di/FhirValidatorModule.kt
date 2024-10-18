/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.validation.FhirValidator
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.validation.ResourceValidationRequestHandler

@Module
@InstallIn(SingletonComponent::class)
class FhirValidatorModule {

  @Provides
  @Singleton
  fun provideFhirValidator(fhirContext: FhirContext): FhirValidator {
    val validationSupportChain =
      ValidationSupportChain(
        DefaultProfileValidationSupport(fhirContext),
        InMemoryTerminologyServerValidationSupport(fhirContext),
        CommonCodeSystemsTerminologyService(fhirContext),
        UnknownCodeSystemWarningValidationSupport(fhirContext).apply {
          setNonExistentCodeSystemSeverity(IValidationSupport.IssueSeverity.WARNING)
        },
      )
    val instanceValidator = FhirInstanceValidator(validationSupportChain)
    instanceValidator.isAssumeValidRestReferences = true
    instanceValidator.invalidateCaches()
    return fhirContext.newValidator().apply { registerValidatorModule(instanceValidator) }
  }

  @Provides
  @Singleton
  fun provideResourceValidationRequestHandler(
    fhirValidatorProvider: Lazy<FhirValidator>,
    dispatcherProvider: DispatcherProvider,
  ): ResourceValidationRequestHandler {
    return ResourceValidationRequestHandler(fhirValidatorProvider.get(), dispatcherProvider)
  }
}
