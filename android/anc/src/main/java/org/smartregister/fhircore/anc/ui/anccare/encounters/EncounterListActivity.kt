package org.smartregister.fhircore.anc.ui.anccare.encounters

import android.os.Bundle
import androidx.activity.compose.setContent
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class EncounterListActivity : BaseMultiLanguageActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewModel = dummyData()
    setContent {
      AppTheme {
        EncounterListScreen(viewModel)
      }
    }
  }
}