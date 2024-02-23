/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.geowidget.model.GeoWidgetLocation
import org.smartregister.fhircore.geowidget.screens.GeoWidgetFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen

@AndroidEntryPoint
class GeoWidgetLauncherFragment : Fragment(R.layout.fragment_geo_widget_launcher) {
  @Inject lateinit var eventBus: EventBus
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var geoWidgetConfiguration: GeoWidgetConfiguration
  private lateinit var geoWidgetFragment: GeoWidgetFragment
  private val geoWidgetLauncherViewModel by viewModels<GeoWidgetLauncherViewModel>()
  private val args by navArgs<GeoWidgetLauncherFragmentArgs>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    geoWidgetConfiguration = geoWidgetConfiguration()
  }

  private fun geoWidgetConfiguration(): GeoWidgetConfiguration =
    configurationRegistry.retrieveConfiguration(
      ConfigType.GeoWidget,
      args.geoWidgetId,
    )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    geoWidgetLauncherViewModel.retrieveLocations()


    geoWidgetFragment = GeoWidgetFragment.builder()
      .setUseGpsOnAddingLocation(false)
      .setOnAddLocationListener { geoWidgetLocation: GeoWidgetLocation ->
        if (geoWidgetLocation.position == null) return@setOnAddLocationListener
        geoWidgetLauncherViewModel.launchQuestionnaireWithParams(
          geoWidgetLocation,
          activity?.tryUnwrapContext() as android.content.Context,
          geoWidgetConfiguration.registrationQuestionnaire,
        )
      }
      .setOnCancelAddingLocationListener {

      }
      .setOnClickLocationListener { geoWidgetLocation: GeoWidgetLocation ->
        // todo: open profile
      }
      .build()

    if (savedInstanceState == null) {
      addGeoWidgetFragment()
    }

    setLocationFromDbCollector()
    setOnQuestionnaireSubmissionListener()
  }
  private fun setOnQuestionnaireSubmissionListener() {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        eventBus.events
          .getFor(MainNavigationScreen.GeoWidgetLauncher.eventId(geoWidgetConfiguration.id))
          .onEach { appEvent ->
            if (appEvent is AppEvent.OnSubmitQuestionnaire) {
              val extractedResourceIds = appEvent.questionnaireSubmission.extractedResourceIds
              geoWidgetLauncherViewModel.onQuestionnaireSubmission(extractedResourceIds)
            }
          }
          .launchIn(lifecycleScope)
      }
    }
  }

  private fun setLocationFromDbCollector() {
    viewLifecycleOwner.lifecycleScope.launch {
      geoWidgetLauncherViewModel.locationsFlow
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { locations ->
          geoWidgetFragment.addLocationsToMap(locations)
        }
    }
  }

  private fun addGeoWidgetFragment() {
    childFragmentManager.commit {
      add(
        R.id.add_geo_widget_container,
        geoWidgetFragment,
        GEO_WIDGET_FRAGMENT_TAG,
      )
    }
  }

  companion object {
    const val GEO_WIDGET_FRAGMENT_TAG = "geo-widget-fragment-tag"
  }
}
