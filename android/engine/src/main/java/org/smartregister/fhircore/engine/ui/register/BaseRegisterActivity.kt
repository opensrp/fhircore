package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.engine.BR
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.databinding.DrawerMenuHeaderBinding
import org.smartregister.fhircore.engine.configuration.view.ConfigurableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.util.extension.viewmodel.createFactory

abstract class BaseRegisterActivity :
    AppCompatActivity(), ConfigurableView<RegisterViewConfiguration> {

  private lateinit var baseRegisterViewModel: BaseRegisterViewModel

  private lateinit var registerActivityBinding: BaseRegisterActivityBinding

  private lateinit var drawerMenuHeaderBinding: DrawerMenuHeaderBinding

  override val configurableViews: Map<String, View> = mutableMapOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    baseRegisterViewModel =
        ViewModelProvider(
            this, BaseRegisterViewModel(registerViewConfigurationOf()).createFactory())[
            BaseRegisterViewModel::class.java]

    baseRegisterViewModel.registerViewConfiguration.observe(this, this::setupConfigurableViews)
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
      toolbarLayout.btnDrawerMenu.setOnClickListener {
        this.drawerLayout.openDrawer(GravityCompat.START)
      }
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

  private fun View.toggleVisibility(show: Boolean) =
      if (show) this.visibility = View.VISIBLE
      else this.visibility = View.GONE
}
