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

package org.smartregister.fhircore.activity.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.fhir.datacapture.views.barcode.mlkit.md.LiveBarcodeScanningFragment
import java.util.Locale
import org.smartregister.fhircore.util.QuestionnaireUtils.buildQuestionnaireIntent
import org.smartregister.fhircore.util.SharedPreferencesHelper
import org.smartregister.fhircore.util.Utils
import timber.log.Timber

/**
 * BaseActivity that setup a basic Activity including
 * - contentLayout,
 * - a toolbar (optional) - override [toolBarId],
 * - barcode scanner (optional) - call [initBarcodeScanner]
 */
abstract class BaseActivity : AppCompatActivity() {
  private val liveBarcodeScanningFragment by lazy { LiveBarcodeScanningFragment() }
  private lateinit var onBarcodeResult: (barcode: String, view: View) -> Unit?

  override fun attachBaseContext(base: Context) {
    val lang: String? =
      SharedPreferencesHelper.read(SharedPreferencesHelper.LANG, Locale.ENGLISH.toLanguageTag())
    val newConfiguration: Configuration? = Utils.setAppLocale(base, lang)
    super.attachBaseContext(base)
    applyOverrideConfiguration(newConfiguration)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Timber.d("Starting BaseActivity")

    setContentView(getContentLayout())

    // setup toolbar if requested
    toolBarId()?.let {
      Timber.d("Setting toolbar for view id $it")

      val toolbar = findViewById<Toolbar>(it)
      setSupportActionBar(toolbar)
    }
  }

  /** set text on given [TextView] item id in toolbar with given string resource id */
  fun setToolbarItemText(@IdRes viewId: Int, toolbar: Toolbar, @StringRes text: Int) {
    toolbar.findViewById<TextView>(viewId).text = getString(text)
  }

  @LayoutRes abstract fun getContentLayout(): Int

  /** Override this if you want to setup a toolbar for this activity */
  @IdRes
  open fun toolBarId(): Int? {
    return null
  }

  fun startQuestionnaire(
    questionnaireTitle: String,
    questionnaireId: String,
    patientId: String?
  ) {
    startActivity(
      buildQuestionnaireIntent(this, questionnaireTitle, questionnaireId, patientId)
    )
  }

  /**
   * Initialize barcode scanner if activity needs to allow barcode scanning.
   *
   * Example of usage
   * ```
   * initBarcodeScanner(R.id.add_client, R.id.search_client) { barcode, view ->
   *    when(view.id) {
   *      R.id.add_client -> addMyClient(barcode)
   *      R.id.search_client -> searchMyClient(barcode)
   *    }
   * }
   * ```
   * @param barcodeScannerViewId the ids of views which should trigger a barcode scanner fragment
   * @param onBarcodeResult the lambda that would be triggered on successful barcode scanning
   */
  fun initBarcodeScanner(
    @IdRes vararg barcodeScannerViewId: Int,
    onBarcodeResult: (barcode: String, view: View) -> Unit
  ) {
    this.onBarcodeResult = onBarcodeResult

    // setup onclick for each barcode view id passed to launch permissions dialogue or scanner
    barcodeScannerViewId.forEach {
      val requestPermissionLauncher = getBarcodePermissionLauncher()

      val barcodeScannerView = findViewById<View>(it)
      barcodeScannerView.setOnClickListener { view ->
        launchBarcodeReader(requestPermissionLauncher)

        // setup on barcode result listener on given view
        barcodeFragmentListener(view)
      }
    }
  }

  private fun barcodeFragmentListener(view: View) {
    supportFragmentManager.setFragmentResultListener(
      "result",
      this,
      { key, result ->
        val barcode = result.getString(key)!!.trim()
        Timber.i("Received barcode $barcode initiated by view id ${view.id}")

        onBarcodeResult(barcode, view)

        liveBarcodeScanningFragment.onDestroy()
      }
    )
  }

  private fun getBarcodePermissionLauncher(): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      isGranted: Boolean ->
      if (isGranted) {
        liveBarcodeScanningFragment.show(supportFragmentManager, "TAG")
      } else {
        Toast.makeText(
            this,
            "Camera permissions are needed to launch barcode reader!",
            Toast.LENGTH_LONG
          )
          .show()
      }
    }
  }

  private fun launchBarcodeReader(requestPermissionLauncher: ActivityResultLauncher<String>) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
      liveBarcodeScanningFragment.show(this.supportFragmentManager, "TAG")
    } else {
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }
}
