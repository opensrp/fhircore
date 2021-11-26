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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent

@Composable
fun ExposedInfantListItem(
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

        Text(
            text = "HCC-${getFormattedIdentifier(patientItem.identifier)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = modifier.height(8.dp))
            Row {
                Text(
                    color = SubtitleTextColor,
                    text = "${patientItem.age}, ${patientItem.genderFull()}",
                    fontSize = 16.sp,
                    modifier = modifier.wrapContentWidth()
                )
            }
        }

        val backgroundColor = if (patientItem.genderFull() == "Male") Color.Blue else Color.Magenta

        Box(contentAlignment = Alignment.Center,
            modifier = modifier
                .size(64.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.ChildCare,
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

fun getFormattedIdentifier(identifier: String): String {
    val lengthOfProperlyFormattedId = 4
    val idLength = identifier.length

    if (idLength == lengthOfProperlyFormattedId) {
        return identifier
    }

    var idPlaceholder = ""
    val numberOfDigitsRemaining = lengthOfProperlyFormattedId - idLength
    for (num in 1..numberOfDigitsRemaining) {
        idPlaceholder += "0"
    }

    return idPlaceholder + identifier
}