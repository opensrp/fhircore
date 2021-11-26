package org.smartregister.fhircore.mwcore.ui.patient.register.components.list_items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent

@Composable
fun ContactListItem(
    patientItem: PatientItem,
    clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Spacer(modifier = modifier.width(8.dp))

        Column(
            modifier =
            modifier
                .clickable { clickListener(OpenPatientProfile, patientItem) }
                .padding(15.dp)
                .weight(0.65f)
        ) {
            Text(
                text = patientItem.name,
                fontSize = 18.sp,
                modifier = modifier.wrapContentWidth(),
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = modifier.height(8.dp))
            Row {
                Text(
                    color = SubtitleTextColor,
                    text = patientItem.genderFull(),
                    fontSize = 16.sp,
                    modifier = modifier.wrapContentWidth()
                )
            }
        }

        val backgroundColor = if (patientItem.genderFull() == "Male") Color.Blue else Color.Magenta

        // TODO: Use the Man and Woman icons instead of Male and Female
        val icon = if (patientItem.genderFull() == "Male") Icons.Default.Male else Icons.Default.Female

        Box(contentAlignment = Alignment.Center,
            modifier = modifier
                .size(64.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "image",
                tint = Color.White,
                modifier = modifier.size(32.dp)
            )
        }

        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "image",
                tint = SubtitleTextColor,
            )
        }
    }
}