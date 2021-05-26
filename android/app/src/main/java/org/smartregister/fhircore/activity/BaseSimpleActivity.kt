package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.viewmodel.BaseViewModel
import timber.log.Timber

abstract class BaseSimpleActivity :
    AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
  private lateinit var viewModel: BaseViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Timber.d("Starting BaseSimpleActivity")

    setContentView(getContentLayout())

    Timber.d("Now setting toolbar and navbar")

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    setNavigationViewListener()

    Timber.d("Now init viewmodel")

    viewModel = ViewModelProvider(this).get(BaseViewModel::class.java)

    Timber.d("Now setting drawer")
    setupDrawer()

    initClientCountObserver()

    loadCounts()
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
      }
    }

    getDrawerLayout().closeDrawer(GravityCompat.START)
    return true
  }

  protected fun setNavigationViewListener() {
    val navigationView = getNavigationView()
    navigationView.setNavigationItemSelectedListener(this)
  }

  protected fun getNavigationView(): NavigationView {
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
        this, Observer { event -> setMenuCounter(R.id.menu_item_clients, event) })
  }

  // TODO look into ways on how to improve performance for this
  private fun loadCounts() {
    viewModel.loadClientCount()
  }
}
