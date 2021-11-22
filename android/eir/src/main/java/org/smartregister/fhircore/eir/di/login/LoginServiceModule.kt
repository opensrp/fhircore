package org.smartregister.fhircore.eir.di.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.smartregister.fhircore.eir.ui.login.EirLoginService
import org.smartregister.fhircore.engine.ui.login.LoginService

@InstallIn(ActivityComponent::class)
@Module
abstract class LoginServiceModule {

  @Binds abstract fun bindLoginService(eirLoginService: EirLoginService): LoginService
}
