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

package org.smartregister.fhircore.mwcore.ui.patient.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent


@Composable
fun MwChip(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colors.onSecondary,
    background: Color = MaterialTheme.colors.secondary
) {
    Box(
        modifier = Modifier.background(
            background,
            RoundedCornerShape(18.dp)
        )
    ) {
        Text(
            color = textColor,
            text = text,
            fontSize = fontSize,
            modifier = modifier
                .wrapContentWidth()
                .padding(8.dp, 2.dp)
        )
    }
}

@Composable
fun ArtChip(text: String,
            fontSize: TextUnit,
            modifier: Modifier = Modifier,
            textColor: Color = MaterialTheme.colors.onSecondary,
            background: Color = MaterialTheme.colors.secondary)
{
    Box(modifier = modifier.background( color = MaterialTheme.colors.secondary.copy(alpha = 0.2F), 
        RoundedCornerShape(4.dp)))
    {
        Text(
            
            color = textColor,
            text = text,
            fontSize = fontSize,
            modifier = modifier
                .wrapContentWidth()
                .padding(8.dp, 2.dp)
        )
    }

}

@Composable
fun PatientRow(
    patientItem: PatientItem,
    clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
        modifier
            .clickable { clickListener(OpenPatientProfile, patientItem) }

            .padding(6.dp),
        elevation = 6.dp,
        shape = MaterialTheme.shapes.medium.copy(
            androidx.compose.foundation.shape.CornerSize(
                16.dp
            )
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp, 4.dp)
        ) {
            
            ArtChip(text = patientItem.identifier, fontSize =22.sp )


            //Adding space between image and the column
            Spacer(modifier = Modifier.size(8.dp))
            Column(
                modifier =
                modifier

                    .padding(15.dp)
                    .weight(0.65f)
            ) {
                Text(
                    text = "${patientItem.name}",
                    fontSize = 18.sp,
                    modifier = modifier.wrapContentWidth(),
                    color = MaterialTheme.colors.secondaryVariant,
                    style = MaterialTheme.typography.subtitle2
                )
                Spacer(modifier = modifier.height(8.dp))
                Row() {
                    MwChip(
                        text = patientItem.age,
                        fontSize = 16.sp,
                        textColor = MaterialTheme.colors.secondary,
                        background = MaterialTheme.colors.secondary.copy(alpha = 0.2F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MwChip(
                        fontSize = 16.sp,
                        text = "[Club]"  ,
                        textColor = MaterialTheme.colors.primary,
                        background = MaterialTheme.colors.primary.copy(alpha = 0.2F)
                    )
                }
            }
            
            clientImage(text = patientItem.genderFull())

        }
    }

}

@Composable
fun clientImage( text: String){
    if ( text == "Male"){
        Image(
            painter = painterResource(
                id = R.drawable.ic_man),
            contentDescription = "Contact profile picture",
            //resizing our profile picture
            modifier = Modifier
                .size(40.dp)

                //shaping the picture
                .clip(CircleShape)
                .background(MaterialTheme.colors.secondary.copy(alpha = .4F))
                .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
        )
    }
    else
    {
       Image(
           painter = painterResource(
               id = R.drawable.ic_woman_),

            contentDescription = "Contact profile picture",

            //resizing our profile picture
            modifier = Modifier
                .size(40.dp)

                //shaping the picture
                .clip(CircleShape)
                .background(color = Color(0xFFFFC0CB)) //Cyan.copy(alpha = .4F )) //MaterialTheme.colors.secondary.copy(alpha = .4F))
                .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
        )
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRowPreview() {
    val patientItem =
        PatientItem(
            id = "my-test-id",
            identifier = "10001",
            name = "John Doe",
            gender = "M",
            age = "27",
            address = "Nairobi"
        )
    LazyColumn() {
        items(10) {
            PatientRow(patientItem = patientItem, { _, _ -> })
        }
    }

}
