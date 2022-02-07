package org.smartregister.fhircore.quest

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.core.internal.deps.dagger.internal.Preconditions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.smartregister.fhircore.quest.ui.HiltTestActivity

/**
 * Created by ndegwamartin on 06/02/2022.
 */

const val THEME_EXTRAS_BUNDLE_KEY = "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY"

@ExperimentalCoroutinesApi
inline fun <reified T : Fragment> launchFragmentInHiltContainer(fragmentArgs: Bundle? = null, themeResId:Int=R.style.FragmentScenarioEmptyFragmentActivityTheme,
                                                                fragmentFactory: FragmentFactory? = null, crossinline action: T.()->Unit = {}){
    val mainActivityIntent = Intent.makeMainActivity(ComponentName(ApplicationProvider.getApplicationContext(),HiltTestActivity::class.java))
        .putExtra(THEME_EXTRAS_BUNDLE_KEY, themeResId)

    ActivityScenario.launch<HiltTestActivity>(mainActivityIntent).onActivity { activity ->
        fragmentFactory?.let {
            activity.supportFragmentManager.fragmentFactory = it
        }

        val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )

        fragment.arguments = fragmentArgs

        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        (fragment as T).action()
    }
}