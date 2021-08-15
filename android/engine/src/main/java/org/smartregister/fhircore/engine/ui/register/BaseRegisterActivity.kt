package org.smartregister.fhircore.engine.ui.register

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import org.smartregister.fhircore.engine.BR
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.ui.model.Language
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.toggleVisibility
import org.smartregister.fhircore.engine.util.extension.viewmodel.createFactory

abstract class BaseRegisterActivity :
  AppCompatActivity(),
  NavigationView.OnNavigationItemSelectedListener,
  ConfigurableView<RegisterViewConfiguration> {

  private lateinit var baseRegisterViewModel: BaseRegisterViewModel

  private lateinit var registerActivityBinding: BaseRegisterActivityBinding

  private lateinit var drawerMenuHeaderBinding: DrawerMenuHeaderBinding

  override val configurableViews: Map<String, View> = mutableMapOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    application.assertIsConfigurable()

    baseRegisterViewModel =
      ViewModelProvider(
        this,
        BaseRegisterViewModel(
            application = application,
            applicationConfiguration = configurableApplication().applicationConfiguration,
            registerViewConfiguration = registerViewConfigurationOf(),
          )
          .createFactory()
      )[BaseRegisterViewModel::class.java]

    baseRegisterViewModel.registerViewConfiguration.observe(this, this::setupConfigurableViews)

    baseRegisterViewModel.run {
      loadLanguages()
      selectedLanguage.observe(
        this@BaseRegisterActivity,
        { updateLanguage(Language(it, Locale.forLanguageTag(it).displayName)) }
      )
      dataSynced.observe(
        this@BaseRegisterActivity,
        { if (it) showToast(getString(R.string.sync_completed)) }
      )
    }

    registerActivityBinding = DataBindingUtil.setContentView(this, R.layout.base_register_activity)
    registerActivityBinding.apply {
      this.setVariable(BR.registerViewModel, baseRegisterViewModel)
      this.lifecycleOwner = this@BaseRegisterActivity
    }

    drawerMenuHeaderBinding =
      DataBindingUtil.bind(registerActivityBinding.navView.getHeaderView(0))!!

    setUpViews()
  }

  private fun setUpViews() {
    with(registerActivityBinding) {
      toolbarLayout.btnDrawerMenu.setOnClickListener { manipulateDrawer(open = true) }
      btnRegisterNewClient.setOnClickListener { registerClient() }
    }
    registerActivityBinding.navView.apply {
      setNavigationItemSelectedListener(this@BaseRegisterActivity)
      menu.findItem(R.id.menu_item_logout).title =
        getString(
          R.string.logout_user,
          configurableApplication().secureSharedPreference.retrieveSessionUsername()
        )
    }

    registerActivityBinding.tvSync.setOnClickListener {
      showToast(getString(R.string.syncing))
      manipulateDrawer(open = false)
      baseRegisterViewModel.syncData()
    }
  }

  private fun manipulateDrawer(open: Boolean = false) {
    with(registerActivityBinding) {
      if (open) drawerLayout.openDrawer(GravityCompat.START)
      else drawerLayout.closeDrawer(GravityCompat.START)
    }
  }

  override fun configureViews(viewConfiguration: RegisterViewConfiguration) {
    baseRegisterViewModel.updateViewConfigurations(viewConfiguration)
  }

  override fun setupConfigurableViews(viewConfiguration: RegisterViewConfiguration) {
    drawerMenuHeaderBinding.tvNavHeader.text = viewConfiguration.appTitle
    val navView = registerActivityBinding.navView

    val languageMenuItem = navView.menu.findItem(R.id.menu_item_language)
    languageMenuItem.isVisible = viewConfiguration.switchLanguages

    with(registerActivityBinding.toolbarLayout) {
      btnShowOverdue.apply {
        toggleVisibility(viewConfiguration.showFilter)
        text = viewConfiguration.filterText
      }
      editTextSearch.apply {
        toggleVisibility(viewConfiguration.showSearchBar)
        hint = viewConfiguration.searchBarHint
      }
      layoutScanBarcode.toggleVisibility(viewConfiguration.showScanQRCode)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return onSideMenuOptionSelected(item)
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_language -> renderSelectLanguageDialog(this)
      R.id.menu_item_logout ->
        configurableApplication().authenticationService.logout(AccountManager.get(this))
    }
    return true
  }

  private fun renderSelectLanguageDialog(context: Activity): AlertDialog {
    val adapter: ArrayAdapter<Language> = getLanguageArrayAdapter()
    val builder =
      getAlertDialogBuilder().apply {
        setTitle(getLanguageDialogTitle())
        setIcon(R.drawable.ic_outline_language_black)
      }
    val dialog =
      builder
        .setAdapter(adapter) { _, index ->
          val language = baseRegisterViewModel.languages[index]
          refreshSelectedLanguage(language, context)
        }
        .create()
    dialog.show()
    return dialog
  }

  @VisibleForTesting
  fun getLanguageArrayAdapter() =
    ArrayAdapter(this, android.R.layout.simple_list_item_1, baseRegisterViewModel.languages)

  @VisibleForTesting fun getAlertDialogBuilder() = AlertDialog.Builder(this)

  @VisibleForTesting fun getLanguageDialogTitle() = this.getString(R.string.select_language)

  private fun refreshSelectedLanguage(language: Language, context: Activity) {
    updateLanguage(language)
    context.setAppLocale(language.tag)
    SharedPreferencesHelper.write(SharedPreferencesHelper.LANG, language.tag)
    context.refresh()
  }

  private fun updateLanguage(language: Language) {
    (registerActivityBinding.navView.menu.findItem(R.id.menu_item_language).actionView as TextView)
      .text = language.displayName
  }

  /**
   * Abstract method to be implemented by the subclass to provide actions for the menu [item]
   * options
   */
  abstract fun onSideMenuOptionSelected(item: MenuItem): Boolean

  /**
   * Abstract method to be implemented by the subclass to provide action for registering new client
   */
  abstract fun registerClient()

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }
}
