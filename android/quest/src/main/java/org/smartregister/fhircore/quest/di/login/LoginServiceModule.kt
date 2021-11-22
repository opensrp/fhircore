package org.smartregister.fhircore.quest.di.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.smartregister.fhircore.engine.ui.login.LoginService
import org.smartregister.fhircore.quest.ui.login.QuestLoginService

@InstallIn(ActivityComponent::class)
@Module
abstract class LoginServiceModule {

  @Binds abstract fun bindLoginService(questLoginService: QuestLoginService): LoginService
}
