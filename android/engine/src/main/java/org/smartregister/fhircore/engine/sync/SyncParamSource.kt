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

package org.smartregister.fhircore.engine.sync


import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.ResourceType
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncParamSource
@Inject
constructor(
    @ApplicationContext val context: Context
) {
    var compListRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    var compConfigRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    var compManifestRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    var compListItemRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    var binaryRequestQue = LinkedList<(Map<ResourceType, Map<String, String>>)>()
    var compListRequestParamForItems = ArrayList<ResTypeId>()
}