package org.smartregister.fhircore.engine.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.setAppLocale

abstract class BaseMultiLanguageActivity : AppCompatActivity() {

  override fun attachBaseContext(baseContext: Context) {
    val lang =
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
        ?: Locale.ENGLISH.toLanguageTag()
    baseContext.setAppLocale(lang).run {
      super.attachBaseContext(baseContext)
      applyOverrideConfiguration(this)
    }
  }
}
