package org.smartregister.fhircore.quest.ui.usersetting.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.R

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun UserSettingInsightView(
    unsyncedResources: List<Pair<String, Int>>,
    syncedResources: List<Pair<String, Int>>,
) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (unsyncedResources.isNullOrEmpty()){stringResource(id = R.string.synced_statistics)}
                        else stringResource(id = R.string.unsynced_resources),
                    modifier = Modifier.padding(11.dp),
                    style = TextStyle(color = Color.Black, fontSize = 20.sp),
                    fontWeight = FontWeight.Light,
                )
                LazyColumn(modifier = Modifier.wrapContentHeight()) {
                    items(unsyncedResources) { unsynced ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp)
                        ) {
                            Text(
                                text = unsynced.first,
                                modifier = Modifier.align(Alignment.CenterStart),
                                fontWeight = FontWeight.Light,
                            )
                            Text(
                                text = unsynced.second.toString(),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }

                        Spacer(modifier = Modifier.padding(1.dp))
                    }

                    items(syncedResources) {synced ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp)
                        ) {
                            Text(
                                text = synced.first,
                                modifier = Modifier.align(Alignment.CenterStart),
                                fontWeight = FontWeight.Light,
                            )
                            Text(
                                text = synced.second.toString(),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }

                    }
                }
                Divider(color = DividerColor)

                Spacer(modifier = Modifier.padding(8.dp))
    }
}

@Preview
@Composable
fun UserSettingInsightViewPreview() {

}
