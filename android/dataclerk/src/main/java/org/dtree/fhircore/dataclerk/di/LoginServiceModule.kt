package org.dtree.fhircore.dataclerk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.smartregister.fhircore.engine.ui.login.LoginService

@InstallIn(ActivityComponent::class)
@Module
abstract class LoginServiceModule {

    @Binds
    abstract fun bindLoginService(loginService: DataClerkServiceModule): LoginService
}
