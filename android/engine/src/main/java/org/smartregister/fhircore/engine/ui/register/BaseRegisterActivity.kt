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
import kotlin.math.ceil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BR
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.register.model.Language
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.DrawablePosition
import org.smartregister.fhircore.engine.util.extension.addOnDrawableClickListener
import org.smartregister.fhircore.engine.util.extension.asString
import org.smartregister.fhircore.engine.util.extension.assertIsConfigurable
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.getDrawable
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.lastSyncDateTime
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale
import org.smartregister.fhircore.engine.util.extension.show
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.toggleVisibility
import timber.log.Timber

abstract class BaseRegisterActivity :
  BaseMultiLanguageActivity(),
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
    registerViewModel.currentPage.observe(
      this,
      { currentPage ->
        selectedMenuOption?.count?.let { recordsCount ->
          updatePaginationViews(recordsCount, currentPage + 1)
        }
      }
    )

    registerViewModel.run {
      loadLanguages()
      selectedLanguage.observe(
        this@BaseRegisterActivity,
        { updateLanguage(Language(it, Locale.forLanguageTag(it).displayName)) }
      )

      lifecycleScope.launch {
        sharedSyncStatus.collect {
          Timber.i("Sync state received is $it")

          when (it) {
            is State.Started -> showToast(getString(R.string.syncing))
            is State.Failed -> {
              showToast(getString(R.string.sync_failed))
              updateLastSyncDateView(it.result.timestamp.asString())
            }
            is State.Finished -> {
              showToast(getString(R.string.sync_completed))
              updateLastSyncDateView(it.result.timestamp.asString())
            }
          }
        }
      }
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

  private fun updateEntityCounts() = sideMenuOptions().forEach { updateCount(it) }

  private fun updateLastSyncDateView(lastSyncDate: String) {
    Timber.i("Updating last sync date $lastSyncDate")
    registerActivityBinding.tvLastSyncTimestamp.text = lastSyncDate
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

    updateLastSyncDateView(application.lastSyncDateTime())

    registerActivityBinding.tvSync.setOnClickListener {
      manipulateDrawer(open = false)
      registerViewModel.runSync()
    }

    // Setup view pager
    registerPagerAdapter = RegisterPagerAdapter(this, supportedFragments = supportedFragments())
    registerActivityBinding.listPager.adapter = registerPagerAdapter

    setupSearchView()
    setupDueButtonView()
    setupPagination()
  }

  private fun setupPagination() {
    with(registerActivityBinding) {
      previousPageButton.setOnClickListener {
        this@BaseRegisterActivity.registerViewModel.backToPreviousPage()
      }
      nextPageButton.setOnClickListener { this@BaseRegisterActivity.registerViewModel.nextPage() }
    }
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
            this.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT) { editable?.clear() }
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
            ) {
              registerViewModel.updateFilterValue(
                RegisterFilterType.SEARCH_FILTER,
                charSequence.toString()
              )
            }

            override fun afterTextChanged(s: Editable?) {}
          }
        )
      }
    }
  }

  private fun setupDueButtonView() {
    with(registerActivityBinding.toolbarLayout) {
      btnShowOverdue.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) registerViewModel.updateFilterValue(RegisterFilterType.OVERDUE_FILTER, true)
        else registerViewModel.updateFilterValue(RegisterFilterType.OVERDUE_FILTER, false)
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
      updateCount(menuOption)
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

  private fun updateCount(menuOption: SideMenuOption) {
    lifecycleScope.launch(registerViewModel.dispatcher.main()) {
      var count: Long = registerViewModel.performCount(menuOption)
      if (count == -1L) count = customEntityCount(menuOption)
      val counter =
        registerActivityBinding.navView.menu.findItem(menuOption.itemId).actionView as TextView
      menuOption.count = count
      counter.text = if (menuOption.count > 0) count.toString() else null

      // Update pagination count
      if (selectedMenuOption != null && menuOption.itemId == selectedMenuOption?.itemId) {
        selectedMenuOption!!.count = menuOption.count
        updatePaginationViews(menuOption.count, registerViewModel.currentPage.value?.plus(1) ?: 1)
      }
    }
  }

  private fun updatePaginationViews(totalRecordsCount: Long, pageNumber: Int) {
    with(registerActivityBinding) {
      val pagesCount =
        ceil(totalRecordsCount.toDouble().div(PaginationUtil.DEFAULT_PAGE_SIZE.toLong())).toLong()
      pageInfoTextview.text = getString(R.string.str_page_info, pageNumber, pagesCount)
      if (pageNumber > 1) previousPageButton.show() else previousPageButton.hide(gone = false)
      if (pageNumber < pagesCount) nextPageButton.show() else nextPageButton.hide(gone = false)
    }
  }
}
