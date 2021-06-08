package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.BR
import org.smartregister.fhircore.R
import org.smartregister.fhircore.databinding.LoginBinding
import org.smartregister.fhircore.viewmodel.LoginViewModel
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

  private lateinit var binding: LoginBinding
  private lateinit var viewModel: LoginViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

    binding = DataBindingUtil.setContentView(this, R.layout.login)
    binding.setVariable(BR.usr, viewModel)
    binding.lifecycleOwner = this

    startHomeObserver()
  }

  private fun startHomeObserver() {
    viewModel.goHome.observe(this, Observer {
      Timber.i("GoHome value changed, now shall start home %b", it)

      if(it){
        val intent = Intent(baseContext, PatientListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
        finish()
      }
    })
  }
}
