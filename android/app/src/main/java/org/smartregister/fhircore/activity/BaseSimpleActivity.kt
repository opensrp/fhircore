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

package org.smartregister.fhircore.activity

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.auth.account.AccountHelper
import org.smartregister.fhircore.auth.secure.SecureConfig
import org.smartregister.fhircore.domain.Language
import org.smartregister.fhircore.util.SharedPreferencesHelper
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.BaseViewModel
import timber.log.Timber

abstract class BaseSimpleActivity :
  MultiLanguageBaseActivity(), NavigationView.OnNavigationItemSelectedListener {
  lateinit var viewModel: BaseViewModel
  lateinit var accountHelper: AccountHelper
  lateinit var secureConfig: SecureConfig

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Timber.d("Starting BaseSimpleActivity")

    setContentView(getContentLayout())

    Timber.d("Now setting toolbar and navbar")

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    setNavigationViewListener()

    Timber.d("Now init viewmodel")

    val fhirEngine: FhirEngine = FhirApplication.fhirEngine(this)

    viewModel =
      ViewModelProvider(this, BaseViewModel.BaseViewModelFactory(application, fhirEngine))
        .get(BaseViewModel::class.java)

    Timber.d("setting account helper")
    accountHelper = AccountHelper(this)

    Timber.d("setting secure config")
    secureConfig = SecureConfig(this)

    Timber.d("Now setting drawer")
    setupDrawer()

    initClientCountObserver()
    initLanguageObserver()

    loadCounts()
    loadLanguages()
    setLogoutUsername()
  }

  abstract fun getContentLayout(): Int

  protected fun getDrawerLayout(): DrawerLayout {
    return findViewById(R.id.drawer_layout)
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    Timber.i("Selected Navbar item %s", item.title)

    when (item.itemId) {
      R.id.menu_item_clients -> {
        startActivity(Intent(baseContext, PatientListActivity::class.java))
        getDrawerLayout().closeDrawer(GravityCompat.START)
      }
      R.id.menu_item_language -> {
        renderSelectLanguageDialog(this)
      }
      R.id.menu_item_logout -> {
        accountHelper.logout(AccountManager.get(this))
        getDrawerLayout().closeDrawer(GravityCompat.START)
      }
    }

    return true
  }

  @VisibleForTesting
  fun getLanguageArrayAdapter() =
    ArrayAdapter(this, android.R.layout.simple_list_item_1, viewModel.languageList)

  @VisibleForTesting fun getAlertDialogBuilder() = AlertDialog.Builder(this@BaseSimpleActivity)

  @VisibleForTesting fun getLanguageDialogTitle() = this.getString(R.string.select_language)

  fun renderSelectLanguageDialog(context: Activity): AlertDialog {

    val adapter: ArrayAdapter<Language> = getLanguageArrayAdapter()

    val builder: AlertDialog.Builder = getAlertDialogBuilder()
    builder.setTitle(getLanguageDialogTitle())
    builder.setIcon(R.drawable.outline_language_black_48)
    val dialog =
      builder
        .setAdapter(adapter) { _, i ->
          val language = viewModel.languageList[i]

          refreshSelectedLanguage(language, context)
        }
        .create()

    dialog.show()

    return dialog
  }

  fun refreshSelectedLanguage(language: Language, context: Activity) {
    setLanguage(language.displayName)

    Utils.setAppLocale(context, language.tag)

    SharedPreferencesHelper.write(SharedPreferencesHelper.LANG, language.tag)

    Utils.refreshActivity(context)
  }

  protected fun setNavigationViewListener() {
    val navigationView = getNavigationView()
    navigationView.setNavigationItemSelectedListener(this)
  }

  fun getNavigationView(): NavigationView {
    return findViewById<View>(R.id.nav_view) as NavigationView
  }

  protected fun setupDrawer() {
    val drawerLayout = getDrawerLayout()

    findViewById<ImageButton>(R.id.btn_drawer_menu).setOnClickListener {
      drawerLayout.openDrawer(GravityCompat.START)
    }
  }

  private fun setMenuCounter(@IdRes itemId: Int, count: Int) {
    val counter = getNavigationView().menu.findItem(itemId).actionView as TextView
    counter.text = if (count > 0) count.toString() else null
  }

  private fun initClientCountObserver() {
    Timber.d("Observing client counts livedata")

    viewModel.covaxClientsCount.observe(
      this,
      { event -> setMenuCounter(R.id.menu_item_clients, event) }
    )
  }

  private fun setLanguage(language: String) {

    (getNavigationView().menu.findItem(R.id.menu_item_language).actionView as TextView).apply {
      text = language
    }
  }

  private fun initLanguageObserver() {
    Timber.d("Observing language livedata")

    viewModel.selectedLanguage.observe(
      this,
      { event -> setLanguage(Locale(event).getDisplayName(Locale.ENGLISH)) }
    )
  }

  // TODO look into ways on how to improve performance for this
  private fun loadCounts() {
    viewModel.loadClientCount()
  }

  private fun loadLanguages() {
    viewModel.loadLanguages()
  }

  fun setLogoutUsername() {

    viewModel.username.observe(
      this,
      {
        if (it.isNotEmpty()) {
          getNavigationView().menu.findItem(R.id.menu_item_logout).title =
            "${getString(R.string.logout_as_user)} $it"
        }
      }
    )
    viewModel.username.value = secureConfig.retrieveSessionUsername()
  }
}
