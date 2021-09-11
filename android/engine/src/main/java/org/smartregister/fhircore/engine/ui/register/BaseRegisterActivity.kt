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

package org.smartregister.fhircore.engine.ui.register

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.State
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BR
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.DrawablePosition
import org.smartregister.fhircore.engine.util.extension.addOnDrawableClickListener
import org.smartregister.fhircore.engine.util.extension.asString
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.getDrawable
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.show
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.toggleVisibility
import timber.log.Timber

abstract class BaseRegisterActivity :
  BaseMultiLanguageActivity(),
  NavigationView.OnNavigationItemSelectedListener,
  ConfigurableView<RegisterViewConfiguration>,
  OnSyncListener {

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
    (application as ConfigurableApplication).syncBroadcaster.registerSyncListener(this)

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

      lifecycleScope.launch { sharedSyncStatus.collect { state -> onSync(state) } }
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

  override fun onResume() {
    super.onResume()
    sideMenuOptions().forEach { updateCount(it) }
  }

  private fun BaseRegisterActivityBinding.updateSyncStatus(state: State) {
    when (state) {
      is State.Started, is State.InProgress -> {
        tvLastSyncTimestamp.text = getString(R.string.syncing_in_progress)
        containerProgressSync.background = null
        progressSync.show()
      }
      is State.Finished -> {
        progressSync.hide()
        tvLastSyncTimestamp.text = state.result.timestamp.asString()
        containerProgressSync.background = containerProgressSync.getDrawable(R.drawable.ic_sync)
      }
      is State.Failed -> {
        progressSync.hide()
        tvLastSyncTimestamp.text = getString(R.string.syncing_failed)
        containerProgressSync.background = containerProgressSync.getDrawable(R.drawable.ic_sync)
      }
      else -> return
    }
  }

  private fun setUpViews() {
    setupSideMenu()
    with(registerActivityBinding) {
      toolbarLayout.btnDrawerMenu.setOnClickListener { manipulateDrawer(open = true) }
      btnRegisterNewClient.setOnClickListener { registerClient() }
      containerProgressSync.setOnClickListener {
        progressSync.show()
        manipulateDrawer(false)
        this@BaseRegisterActivity.registerViewModel.runSync()
      }
    }
    registerActivityBinding.navView.apply {
      setNavigationItemSelectedListener(this@BaseRegisterActivity)
      menu.findItem(R.id.menu_item_logout).title =
        getString(
          R.string.logout_user,
          configurableApplication().secureSharedPreference.retrieveSessionUsername()
        )
    }

    // Setup view pager
    registerPagerAdapter = RegisterPagerAdapter(this, supportedFragments = supportedFragments())
    registerActivityBinding.listPager.adapter = registerPagerAdapter

    setupSearchView()
    setupDueButtonView()
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setupSearchView() {
    with(registerActivityBinding.toolbarLayout) {
      editTextSearch.run {
        doAfterTextChanged { editable: Editable? ->
          if (editable?.isEmpty() == true) {
            setOnTouchListener(null)
            setCompoundDrawablesWithIntrinsicBounds(
              this.getDrawable(R.drawable.ic_search),
              null,
              null,
              null
            )
          } else {
            setCompoundDrawablesWithIntrinsicBounds(
              null,
              null,
              getDrawable(R.drawable.ic_cancel),
              null
            )
            this.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT) {
              editable?.clear()
              registerViewModel.updateFilterValue(RegisterFilterType.SEARCH_FILTER, null)
            }
          }
        }
        addTextChangedListener(
          object : TextWatcher {
            override fun beforeTextChanged(
              charSequence: CharSequence?,
              start: Int,
              count: Int,
              after: Int
            ) {}

            override fun onTextChanged(
              charSequence: CharSequence?,
              start: Int,
              before: Int,
              count: Int
            ) {}

            override fun afterTextChanged(editable: Editable?) {
              registerViewModel.updateFilterValue(
                RegisterFilterType.SEARCH_FILTER,
                if (editable.isNullOrEmpty()) null else editable.toString()
              )
            }
          }
        )
      }
    }
  }

  private fun setupDueButtonView() {
    with(registerActivityBinding.toolbarLayout) {
      btnShowOverdue.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          registerViewModel.updateFilterValue(RegisterFilterType.OVERDUE_FILTER, true)
        } else {
          registerViewModel.updateFilterValue(RegisterFilterType.OVERDUE_FILTER, null)
        }
      }
    }
  }

  private fun setupSideMenu() {
    sideMenuOptionMap = sideMenuOptions().associateBy { it.itemId }
    if (sideMenuOptionMap.size == 1) selectedMenuOption = sideMenuOptionMap.values.elementAt(0)
    val menu = registerActivityBinding.navView.menu

    sideMenuOptions().forEach { menuOption ->
      menu.add(R.id.menu_group_clients, menuOption.itemId, Menu.NONE, menuOption.titleResource)
        .apply {
          icon = menuOption.iconResource
          actionView = layoutInflater.inflate(R.layout.drawable_menu_item_layout, null, false)
        }
      if (menuOption.opensMainRegister) {
        mainRegisterSideMenuOption = sideMenuOptionMap[menuOption.itemId]
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
    menu.add(
      R.id.menu_group_empty,
      R.id.menu_group_empty_item_id,
      2,
      ""
    ) // Hack to add last menu divider

    updateRegisterTitle()
  }

  private fun manipulateDrawer(open: Boolean = false) {
    with(registerActivityBinding) {
      if (open) drawerLayout.openDrawer(GravityCompat.START)
      else drawerLayout.closeDrawer(GravityCompat.START)
    }
  }

  override fun onSync(state: State) {
    Timber.i("Sync state received is $state")
    when (state) {
      is State.Started -> {
        showToast(getString(R.string.syncing))
        registerActivityBinding.updateSyncStatus(state)
      }
      is State.Failed, is State.Glitch -> {
        showToast(getString(R.string.sync_failed))
        registerActivityBinding.updateSyncStatus(state)
        this.registerViewModel.setSyncing(false)
      }
      is State.Finished -> {
        showToast(getString(R.string.sync_completed))
        registerActivityBinding.updateSyncStatus(state)
        sideMenuOptions().forEach { updateCount(it) }
        manipulateDrawer(open = false)
        this.registerViewModel.setSyncing(false)
        this.registerViewModel.setRefreshRegisterData(true)
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        // TODO fix issue where state is not updating from in progress
        // this.registerViewModel.setSyncing(true)
        registerActivityBinding.updateSyncStatus(state)
      }
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

  /** List of [SideMenuOption] representing individual menu items listed in the DrawerLayout */
  abstract fun sideMenuOptions(): List<SideMenuOption>

  /**
   * Abstract method to be implemented by the subclass to provide actions for the menu [item]
   * options. Refer to the selected menu item using the view tag that was set by [sideMenuOptions]
   */
  abstract fun onSideMenuOptionSelected(item: MenuItem): Boolean

  protected open fun registerClient() {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.requiredIntentArgs(
            clientIdentifier = null,
            form = registerViewModel.registerViewConfiguration.value?.registrationForm!!
          )
        )
    )
  }

  /**
   * Implement this method to provide the view pager with a list of [Fragment]. App will throw an
   * exception if you attempt to use [BaseRegisterActivity] without at least one subclass of
   * [BaseRegisterFragment]
   */
  abstract fun supportedFragments(): List<Fragment>

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }

  private fun updateCount(sideMenuOption: SideMenuOption) {
    lifecycleScope.launch(registerViewModel.dispatcher.main()) {
      val count = sideMenuOption.countMethod()
      if (count == -1L) {
        sideMenuOption.count =
          (application as ConfigurableApplication).fhirEngine.countActivePatients()
      } else {
        sideMenuOption.count = count
      }
      with(sideMenuOption) {
        val counter =
          registerActivityBinding.navView.menu.findItem(sideMenuOption.itemId).actionView as
            TextView
        counter.text = if (this.count > 0) this.count.toString() else null
        if (selectedMenuOption != null && this.itemId == selectedMenuOption?.itemId) {
          selectedMenuOption?.count = this.count
        }
      }
    }
  }
}
