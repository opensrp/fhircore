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

package org.smartregister.fhircore.quest.robolectric

import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.util.inject.Injector

internal class FhircoreTestRunner : RobolectricTestRunner {
  constructor(testClass: Class<*>?) : super(testClass) {}
  constructor(testClass: Class<*>?, injector: Injector?) : super(testClass, injector) {}

  override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration {
    val config = super.createClassLoaderConfig(method)
    val builder = InstrumentationConfiguration.Builder(config)
    builder.doNotInstrumentPackage("androidx.fragment")
    builder.doNotInstrumentPackage("org.hl7.fhir.r4")
    return builder.build()
  }
}
