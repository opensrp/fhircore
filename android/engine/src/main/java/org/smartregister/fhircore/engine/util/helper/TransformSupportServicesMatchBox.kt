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

package org.smartregister.fhircore.engine.util.helper

import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.elementmodel.Manager
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceFactory
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.r4.utils.StructureMapUtilities.ITransformerServices
import org.slf4j.LoggerFactory

/*
 * #%L
 * Matchbox Engine
 * %%
 * Copyright (C) 2022 ahdis
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

@Singleton
class TransformSupportServicesMatchBox @Inject constructor(val context: SimpleWorkerContext) :
  ITransformerServices {

  override fun createType(appInfo: Any, name: String): Base {
    return try {
      val sd = context.fetchResource(StructureDefinition::class.java, name)
      Manager.build(context, sd)
    } catch (ex: Exception) {
      ResourceFactory.createResourceOrType(name)
    }
  }

  override fun createResource(appInfo: Any, res: Base, atRootofTransform: Boolean): Base {
    return res
  }

  @Throws(FHIRException::class)
  override fun translate(appInfo: Any, source: Coding, conceptMapUrl: String): Coding {
    val cme = ConceptMapEngine(context)
    return cme.translate(source, conceptMapUrl)
  }

  @Throws(FHIRException::class)
  override fun resolveReference(appContext: Any, url: String): Base {
    return context.fetchResource(Resource::class.java, url)
    //    if (resource != null) {
    //      String inStr =
    // FhirContext.forR4Cached().newJsonParser().encodeResourceToString(resource);
    //      try {
    //        return Manager.parseSingle(context, new ByteArrayInputStream(inStr.getBytes()),
    // FhirFormat.JSON);
    //      } catch (IOException e) {
    //        throw new FHIRException("Cannot convert resource to element model");
    //      }
    //    }
    //    throw new FHIRException("resolveReference, url not found: " + url);
  }

  @Throws(FHIRException::class)
  override fun performSearch(appContext: Any, url: String): List<Base> {
    throw FHIRException("performSearch is not supported yet")
  }

  override fun log(message: String) {
    log.debug(message)
  }

  companion object {
    protected val log = LoggerFactory.getLogger(TransformSupportServices::class.java)
  }
}
