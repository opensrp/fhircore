package org.smartregister.fhircore.engine.app.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.engine.app.AppLoginService

@InstallIn(ActivityComponent::class)
@Module
abstract class LoginServiceModule {

  @Binds abstract fun bindLoginService(appLoginService: AppLoginService): LoginService
}
