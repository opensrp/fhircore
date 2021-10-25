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
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.State
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import java.text.ParseException
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncInitiator
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.navigation.NavigationBottomSheet
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.DrawablePosition
import org.smartregister.fhircore.engine.util.extension.addOnDrawableClickListener
import org.smartregister.fhircore.engine.util.extension.asString
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
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
  NavigationBarView.OnItemSelectedListener,
  ConfigurableView<RegisterViewConfiguration>,
  OnSyncListener,
  SyncInitiator {

  private lateinit var registerViewModel: RegisterViewModel

  private lateinit var registerActivityBinding: BaseRegisterActivityBinding

  override val configurableViews: Map<String, View> = mutableMapOf()

  private var selectedMenuOption: SideMenuOption? = null

  private lateinit var sideMenuOptionMap: Map<Int, SideMenuOption>

  protected lateinit var fhirEngine: FhirEngine

  protected lateinit var navigationBottomSheet: NavigationBottomSheet

  private lateinit var supportedFragments: Map<String, Fragment>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    application.assertIsConfigurable()
    val syncBroadcaster = (application as ConfigurableApplication).syncBroadcaster
    syncBroadcaster.registerSyncListener(this)

    supportedFragments = supportedFragments()

    registerViewModel =
      ViewModelProvider(
        this,
        RegisterViewModel(
            application = application,
            registerViewConfiguration = registerViewConfigurationOf(),
          )
          .createFactory()
      )[RegisterViewModel::class.java]

    // Initiate sync after registerViewModel is initialized
    syncBroadcaster.registerSyncInitiator(this)

    registerViewModel.registerViewConfiguration.observe(this, this::setupConfigurableViews)

    registerViewModel.lastSyncTimestamp.observe(
      this,
      {
        registerActivityBinding.btnRegisterNewClient.isEnabled = !it.isNullOrEmpty()
        registerActivityBinding.tvLastSyncTimestamp.text = it?.formatSyncDate() ?: ""
      }
    )

    registerViewModel.run {
      loadLanguages()
      selectedLanguage.observe(
        this@BaseRegisterActivity,
        { updateLanguage(Language(it, Locale.forLanguageTag(it).displayName)) }
      )

      lifecycleScope.launch { sharedSyncStatus.collect { state -> onSync(state) } }
    }

    registerActivityBinding = DataBindingUtil.setContentView(this, R.layout.base_register_activity)
    registerActivityBinding.lifecycleOwner = this

    fhirEngine = (application as ConfigurableApplication).fhirEngine

    navigationBottomSheet = NavigationBottomSheet.getInstance(this::onSelectRegister)
  }

  override fun onResume() {
    super.onResume()
    sideMenuOptions().forEach { updateCount(it) }
  }

  private fun BaseRegisterActivityBinding.updateSyncStatus(state: State) {
    when (state) {
      is State.Started, is State.InProgress -> {
        progressSync.show()
        tvLastSyncTimestamp.text = getString(R.string.syncing_in_progress)
        containerProgressSync.apply {
          background = null
          setOnClickListener(null)
        }
      }
      is State.Finished, is State.Failed -> setLastSyncTimestamp(state)
      is State.Glitch -> {
        progressSync.hide()
        val lastSyncTimestamp =
          SharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, getString(R.string.syncing_retry))
        tvLastSyncTimestamp.text = lastSyncTimestamp?.formatSyncDate() ?: ""
        containerProgressSync.apply {
          background = this.getDrawable(R.drawable.ic_sync)
          setOnClickListener { syncButtonClick() }
        }
      }
    }
  }

  private fun BaseRegisterActivityBinding.setLastSyncTimestamp(state: State) {
    val syncTimestamp =
      when (state) {
        is State.Finished -> state.result.timestamp.asString()
        is State.Failed -> state.result.timestamp.asString()
        else -> ""
      }
    progressSync.hide()
    registerViewModel.setLastSyncTimestamp(syncTimestamp)
    containerProgressSync.apply {
      background = containerProgressSync.getDrawable(R.drawable.ic_sync)
      setOnClickListener { syncButtonClick() }
    }
  }

  private fun setUpViews() {
    setupNavigation(registerViewModel.registerViewConfiguration.value!!)

    with(registerActivityBinding) {
      toolbarLayout.btnDrawerMenu.setOnClickListener { manipulateDrawer(open = true) }
      btnRegisterNewClient.setOnClickListener { registerClient() }
      containerProgressSync.setOnClickListener { syncButtonClick() }
    }

    setupNewClientButtonView(registerViewModel.registerViewConfiguration.value!!)

    updateRegisterTitle()

    setupSearchView()

    setupDueButtonView()

    switchFragment(mainFragmentTag())
  }

  private fun syncButtonClick() {
    registerActivityBinding.progressSync.show()
    manipulateDrawer(false)
    registerViewModel.runSync()
  }

  private fun String.formatSyncDate(): String {
    if (this.isEmpty()) return ""
    val date =
      try {
        Date(this)
      } catch (parseException: ParseException) {
        Timber.e(parseException)
        null
      } ?: return ""
    val formattedDate: String = DateUtils.simpleDateFormat().format(date)
    return getString(R.string.last_sync_timestamp, formattedDate)
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

  private fun setupNewClientButtonView(registerViewConfiguration: RegisterViewConfiguration) {
    with(registerActivityBinding.btnRegisterNewClient) {
      this.setOnClickListener { registerClient() }
      if (registerViewConfiguration.newClientButtonStyle.isNotEmpty()) {
        this.background = getDrawable(registerViewConfiguration.newClientButtonStyle)
      }
      this.text = registerViewConfiguration.newClientButtonText
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

  private fun setupNavigation(viewConfiguration: RegisterViewConfiguration) {
    if (!viewConfiguration.showSideMenu) {
      with(registerActivityBinding) {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolbarLayout.apply {
          btnDrawerMenu.hide(true)
          topToolbarSection.toggleVisibility(!viewConfiguration.showSideMenu)
          middleToolbarSection.toggleVisibility(viewConfiguration.showSideMenu)
        }
      }
    }

    val drawerMenuHeaderBinding: DrawerMenuHeaderBinding =
      DataBindingUtil.bind(registerActivityBinding.navView.getHeaderView(0))!!
    drawerMenuHeaderBinding.tvNavHeader.text = viewConfiguration.appTitle

    setupSideMenu()

    val languageMenuItem = findSideMenuItem(R.id.menu_item_language)!!
    languageMenuItem.isVisible = viewConfiguration.switchLanguages

    registerActivityBinding.navView.apply {
      setNavigationItemSelectedListener(this@BaseRegisterActivity)
      menu.findItem(R.id.menu_item_logout)?.title =
        getString(
          R.string.logout_user,
          configurableApplication().secureSharedPreference.retrieveSessionUsername()
        )
    }
  }

  private fun setupSideMenu() {
    sideMenuOptionMap = sideMenuOptions().associateBy { it.itemId }

    val menu = registerActivityBinding.navView.menu

    sideMenuOptions().forEach { menuOption ->
      menu.add(R.id.menu_group_clients, menuOption.itemId, Menu.NONE, menuOption.titleResource)
        .apply {
          icon = menuOption.iconResource
          actionView = layoutInflater.inflate(R.layout.drawable_menu_item_layout, null, false)
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
        sideMenuOptions().forEach { updateCount(it) }
        manipulateDrawer(open = false)
        this.registerViewModel.setRefreshRegisterData(true)
      }
      is State.Finished -> {
        showToast(getString(R.string.sync_completed))
        registerActivityBinding.updateSyncStatus(state)
        sideMenuOptions().forEach { updateCount(it) }
        manipulateDrawer(open = false)
        this.registerViewModel.setRefreshRegisterData(true)
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        registerActivityBinding.updateSyncStatus(state)
      }
    }
  }

  override fun runSync() {
    registerViewModel.runSync()
  }

  override fun configureViews(viewConfiguration: RegisterViewConfiguration) {
    registerViewModel.updateViewConfigurations(viewConfiguration)
  }

  override fun setupConfigurableViews(viewConfiguration: RegisterViewConfiguration) {
    setUpViews()

    with(registerActivityBinding.toolbarLayout) {
      btnShowOverdue.apply {
        toggleVisibility(viewConfiguration.showFilter)
        text = viewConfiguration.filterText
      }
      editTextSearch.apply {
        toggleVisibility(viewConfiguration.showSearchBar)
        hint = viewConfiguration.searchBarHint
      }
      btnScanBarcode.toggleVisibility(viewConfiguration.showScanQRCode)
    }

    setupBottomNavigationMenu(viewConfiguration)
  }

  open fun setupBottomNavigationMenu(viewConfiguration: RegisterViewConfiguration) {
    val bottomMenu = registerActivityBinding.bottomNavView.menu
    registerActivityBinding.bottomNavView.apply {
      toggleVisibility(viewConfiguration.showBottomMenu)
      setOnItemSelectedListener(this@BaseRegisterActivity)
    }
    for ((index, it) in bottomNavigationMenuOptions().withIndex()) {
      bottomMenu.add(R.id.menu_group_default_item_id, it.id, index, it.title).apply {
        it.iconResource.let { icon -> this.icon = icon }
      }
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    selectedMenuOption = sideMenuOptionMap[item.itemId]
    updateRegisterTitle()

    when (item.itemId) {
      R.id.menu_item_language -> renderSelectLanguageDialog(this)
      R.id.menu_item_logout -> {
        configurableApplication().authenticationService.logout()
        manipulateDrawer(open = false)
      }
      else -> {
        manipulateDrawer(open = false)
        return onNavigationOptionItemSelected(item)
      }
    }
    return true
  }

  private fun updateRegisterTitle() {
    registerActivityBinding.toolbarLayout.tvClientsListTitle.text =
      selectedMenuOption?.titleResource?.let { getString(it) }
        ?: registerViewModel.registerViewConfiguration.value?.appTitle
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
    findSideMenuItem(R.id.menu_item_language)?.let {
      (it.actionView as TextView).text = language.displayName
    }
  }

  fun findSideMenuItem(@IdRes id: Int): MenuItem? =
    registerActivityBinding.navView.menu.findItem(id)

  open fun switchFragment(
    tag: String,
    isRegisterFragment: Boolean = true,
    toolbarTitle: String? = null
  ) {
    registerActivityBinding.btnRegisterNewClient.toggleVisibility(tag == mainFragmentTag())
    if (supportedFragments.isEmpty() && !supportedFragments.containsKey(tag)) {
      throw IllegalAccessException("No fragment exists with the tag $tag")
    }

    val hasMultipleRegisterFragments =
      supportedFragments.count { it.value is BaseRegisterFragment<*, *> } >= 2

    // Should be able to switch register fragments or update screen title
    if (isRegisterFragment && hasMultipleRegisterFragments) {
      navigationBottomSheet.registersList =
        registersList().onEach { registerItem ->
          registerItem.isSelected = false
          if (registerItem.uniqueTag.equals(tag, true)) {
            registerItem.isSelected = true
            registerActivityBinding.toolbarLayout.registerFilterTextview.text = registerItem.title
          }
        }

      registerActivityBinding.toolbarLayout.registerFilterTextview.apply {
        setCompoundDrawablesWithIntrinsicBounds(
          null,
          null,
          this.getDrawable(R.drawable.ic_dropdown_arrow),
          null
        )
        setOnClickListener {
          supportFragmentManager.commit { remove(navigationBottomSheet) }
          navigationBottomSheet.show(supportFragmentManager, NavigationBottomSheet.TAG)
        }
      }
    } else if (!toolbarTitle.isNullOrEmpty()) {
      registerActivityBinding.toolbarLayout.registerFilterTextview.apply {
        text = toolbarTitle
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        setOnClickListener(null)
      }
    }

    // Show searchbar/filter button for registers, hide otherwise
    registerActivityBinding.toolbarLayout.apply {
      filterRegisterButton.toggleVisibility(isRegisterFragment)
      bottomToolbarSection.toggleVisibility(isRegisterFragment)
    }

    supportFragmentManager.commit {
      replace(R.id.register_content, supportedFragments.getValue(tag), tag)
    }
  }

  /** List of [SideMenuOption] representing individual menu items listed in the DrawerLayout */
  open fun sideMenuOptions(): List<SideMenuOption> {
    return emptyList()
  }

  /** List of [SideMenuOption] representing individual menu items listed in the DrawerLayout */
  open fun bottomNavigationMenuOptions(): List<NavigationMenuOption> {
    return emptyList()
  }

  /**
   * Override this method to provide a pair of register fragment tag plus their title This MUST be
   * implemented when bottom navigation is used.
   */
  open fun registersList(): List<RegisterItem> {
    return emptyList()
  }

  /**
   * Abstract method to be implemented by the subclass to provide actions for the menu [item]
   * options. Refer to the selected menu item using the view tag that was set by [sideMenuOptions]
   * or [bottomNavigationMenuOptions]
   */
  open fun onNavigationOptionItemSelected(item: MenuItem): Boolean = true

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
   * Implement this method to provide the list of [Fragment] used on the register. App will throw an
   * exception when you attempt to use [BaseRegisterActivity] without at least one subclass of
   * [BaseRegisterFragment]
   */
  abstract fun supportedFragments(): Map<String, Fragment>

  /** Open the fragment identified with [fragmentTag from the map of [supportedFragments] */
  open fun onSelectRegister(fragmentTag: String) {
    navigationBottomSheet.dismiss()
    switchFragment(fragmentTag)
  }

  /**
   * Provide the tag for the main fragment. A register activity MUST have a least one
   * [BaseRegisterFragment]
   */
  abstract fun mainFragmentTag(): String

  override fun configurableApplication(): ConfigurableApplication {
    return application as ConfigurableApplication
  }

  fun updateCount(sideMenuOption: SideMenuOption) {
    lifecycleScope.launch(registerViewModel.dispatcher.main()) {
      val count = sideMenuOption.countMethod()

      with(sideMenuOption) {
        this.count = count

        findSideMenuItem(sideMenuOption.itemId)?.let {
          (it.actionView as TextView).text = if (this.count > 0) this.count.toString() else ""
        }
        if (selectedMenuOption != null && this.itemId == selectedMenuOption?.itemId) {
          selectedMenuOption?.count = this.count
        }
      }
    }
  }
}
