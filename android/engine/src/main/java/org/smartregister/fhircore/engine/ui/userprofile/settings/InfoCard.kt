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

package org.smartregister.fhircore.engine.ui.userprofile.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.smartregister.fhircore.engine.ui.userprofile.ProfileData
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileViewModel

@Composable
fun InfoCard(viewModel: UserProfileViewModel) {
    val data by viewModel.data.observeAsState()

    if (data != null)
        Card(Modifier.padding(6.dp)) {
            Column(Modifier.padding(10.dp)) {
                generateData(data!!).forEach {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(text = it.key)
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = it.value)
                    }
                }
            }
        }
}

fun generateData(data: ProfileData): Map<String, String> {
    val map = mutableMapOf<String, String>()
data.practitionerDetails?.let {
   val details = it.fhirPractitionerDetails
    map["Location"] = details.locations.firstOrNull()?.name ?: ""
    map.put("Organisation", details.organizations.firstOrNull()?.name ?: "")
    map.put("CareTeam",  details.careTeams.firstOrNull()?.name ?: "")
}
    return  map
}
