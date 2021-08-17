package org.smartregister.fhircore.engine.ui.register

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BR
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.ui.model.Language
import org.smartregister.fhircore.engine.ui.model.SideMenuOption
import org.smartregister.fhircore.engine.ui.model.SyncStatus
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

  private lateinit var registerViewModel: RegisterViewModel

  private lateinit var registerActivityBinding: BaseRegisterActivityBinding

  private lateinit var drawerMenuHeaderBinding: DrawerMenuHeaderBinding

  override val configurableViews: Map<String, View> = mutableMapOf()

  private var mainRegisterSideMenuOption: SideMenuOption? = null

  private var selectedMenuOption: SideMenuOption? = null

  private lateinit var sideMenuOptionMap: Map<Int, SideMenuOption>

  private lateinit var registerPagerAdapter: RegisterPagerAdapter

  protected lateinit var fhirEngine: FhirEngine

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    application.assertIsConfigurable()

    registerViewModel =
      ViewModelProvider(
        this,
        RegisterViewModel(
            application = application,
            registerViewConfiguration = registerViewConfigurationOf(),
          )
          .createFactory()
      )[RegisterViewModel::class.java]

    registerViewModel.registerViewConfiguration.observe(this, this::setupConfigurableViews)

    registerViewModel.run {
      loadLanguages()
      selectedLanguage.observe(
        this@BaseRegisterActivity,
        { updateLanguage(Language(it, Locale.forLanguageTag(it).displayName)) }
      )
      syncStatus.observe(
        this@BaseRegisterActivity,
        {
          when (it) {
            SyncStatus.COMPLETE -> {
              showToast(getString(R.string.sync_completed))
              updateEntityCounts()
            }
            SyncStatus.FAILED -> showToast(getString(R.string.sync_failed))
            else -> return@observe
          }
        }
      )
    }

    registerActivityBinding = DataBindingUtil.setContentView(this, R.layout.base_register_activity)
    registerActivityBinding.apply {
      this.setVariable(BR.registerViewModel, registerViewModel)
      this.lifecycleOwner = this@BaseRegisterActivity
    }

    drawerMenuHeaderBinding =
      DataBindingUtil.bind(registerActivityBinding.navView.getHeaderView(0))!!

    fhirEngine = (application as ConfigurableApplication).fhirEngine

    setUpViews()
  }

  private fun setUpViews() {
    setupSideMenu()
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
      registerViewModel.syncData()
    }

    // Setup view pager
    registerPagerAdapter = RegisterPagerAdapter(this, supportedFragments = supportedFragments())
    registerActivityBinding.listPager.adapter = registerPagerAdapter
  }

  private fun setupSideMenu() {
    sideMenuOptionMap = sideMenuOptions().associateBy { it.itemId }
    if (sideMenuOptionMap.size == 1) selectedMenuOption = sideMenuOptionMap.values.elementAt(0)
    val menu = registerActivityBinding.navView.menu

    sideMenuOptions().forEach {
      menu.add(R.id.menu_group_clients, it.itemId, Menu.NONE, it.titleResource).apply {
        icon = it.iconResource
        actionView = layoutInflater.inflate(R.layout.drawable_menu_item_layout, null, false)
      }
      if (it.opensMainRegister) {
        mainRegisterSideMenuOption = sideMenuOptionMap[it.itemId]
        selectedMenuOption = mainRegisterSideMenuOption
      }
    }

    // Add language and logout menu items
    menu.add(R.id.menu_group_app_actions, R.id.menu_item_language, 0, R.string.language).apply {
      icon =
        ContextCompat.getDrawable(this@BaseRegisterActivity, R.drawable.ic_outline_language_white)
      actionView = layoutInflater.inflate(R.layout.drawer_menu_language_layout, null, false)
    }
    menu.add(R.id.menu_group_app_actions, R.id.menu_item_logout, 1, R.string.logout_as_user).apply {
      icon = ContextCompat.getDrawable(this@BaseRegisterActivity, R.drawable.ic_logout_white)
    }
    menu.add(R.id.menu_group_empty, MENU_GROUP_EMPTY, 2, "") // Hack to add last menu divider

    updateRegisterTitle()

    updateEntityCounts()
  }

  private fun manipulateDrawer(open: Boolean = false) {
    with(registerActivityBinding) {
      if (open) drawerLayout.openDrawer(GravityCompat.START)
      else drawerLayout.closeDrawer(GravityCompat.START)
    }
  }

  override fun configureViews(viewConfiguration: RegisterViewConfiguration) {
    registerViewModel.updateViewConfigurations(viewConfiguration)
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

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    selectedMenuOption = sideMenuOptionMap[item.itemId]
    updateRegisterTitle()

    when (item.itemId) {
      mainRegisterSideMenuOption?.itemId -> {
        registerActivityBinding.listPager.currentItem = 0
        manipulateDrawer(open = false)
      }
      R.id.menu_item_language -> renderSelectLanguageDialog(this)
      R.id.menu_item_logout -> {
        configurableApplication().authenticationService.logout(AccountManager.get(this))
        manipulateDrawer(open = false)
      }
      else -> {
        manipulateDrawer(open = false)
        return onSideMenuOptionSelected(item)
      }
    }
    return true
  }

  private fun updateRegisterTitle() {
    registerActivityBinding.toolbarLayout.tvClientsListTitle.text =
      selectedMenuOption?.titleResource?.let { getString(it) }
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
          val language = registerViewModel.languages[index]
          refreshSelectedLanguage(language, context)
        }
        .create()
    dialog.show()
    return dialog
  }

  @VisibleForTesting
  fun getLanguageArrayAdapter() =
    ArrayAdapter(this, android.R.layout.simple_list_item_1, registerViewModel.languages)

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

  abstract fun sideMenuOptions(): List<SideMenuOption>

  /**
   * Abstract method to be implemented by the subclass to provide actions for the menu [item]
   * options. Refer to the selected menu item using the view tag that was set by [sideMenuOptions]
   */
  abstract fun onSideMenuOptionSelected(item: MenuItem): Boolean

  /**
   * Abstract method to be implemented by the subclass to provide action for registering new client
   */
  abstract fun registerClient()

  /**
   * Implement this method to provide the view pager with a list of [Fragment]. App will throw an
   * exception if you attempt to use [BaseRegisterActivity] without at least one subclass of
   * [BaseRegisterFragment]
   */
  abstract fun supportedFragments(): List<Fragment>

  /**
   * Implement [customEntityCount] to count other resource types other than Patient resource. Use
   * the class type of the entity specified in [sideMenuOption]. This is useful for complex count
   * queries
   */
  protected open fun customEntityCount(sideMenuOption: SideMenuOption): Long = 0

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }

  companion object {
    const val MENU_GROUP_EMPTY = 1111
  }

  private fun updateEntityCounts() {
    sideMenuOptions().forEach { menuOption ->
      registerViewModel.viewModelScope.launch(registerViewModel.dispatcher.main()) {
        var count = registerViewModel.performCount(menuOption)
        if (count == -1L) count = customEntityCount(menuOption)
        val counter =
          registerActivityBinding.navView.menu.findItem(menuOption.itemId).actionView as TextView
        counter.text = if (count > 0) count.toString() else null
      }
    }
  }
}
