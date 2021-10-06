package org.smartregister.fhircore.engine.shadow.activity

import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowActivity
import org.smartregister.fhircore.engine.ui.login.BaseLoginActivity

@Implements(BaseLoginActivity::class) class ShadowLoginActivity : ShadowActivity() {}
