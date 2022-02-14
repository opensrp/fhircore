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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun PatientChildRow(
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


            /*MwChip(
                modifier = modifier.background( color = MaterialTheme.colors.secondary.copy(alpha = 0.2F), RoundedCornerShape(4.dp)) ,
                text = patientItem.identifier,
                fontSize = 22.sp,
                textColor = MaterialTheme.colors.onSecondary,
            ) */


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
                        textColor = MaterialTheme.colors.primary,
                        background = MaterialTheme.colors.primary.copy(alpha = 0.2F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MwChip(
                        fontSize = 16.sp,
                        text = "MIP",
                        textColor = MaterialTheme.colors.primary,
                        background = MaterialTheme.colors.primary.copy(alpha = 0.2F)
                    )
                }
            }
        childClientImage(text = patientItem.genderFull())
        
        }
    }

}

@Composable
fun childClientImage( text: String){
    if ( text == "Male"){
        Image(
                painter = painterResource(
                        id = R.drawable.ic_child),
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
                        id = R.drawable.ic_child),

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
fun PatientChildRowPreview() {
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
            PatientChildRow(patientItem = patientItem, { _, _ -> })
        }
    }

}
