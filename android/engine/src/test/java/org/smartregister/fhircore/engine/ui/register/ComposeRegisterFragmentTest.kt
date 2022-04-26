/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.register

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.spyk
import java.time.OffsetDateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.ListenerIntent

@HiltAndroidTest
class ComposeRegisterFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityScenarioRule = ActivityScenarioRule(HiltActivityForTest::class.java)

  val defaultRepository: DefaultRepository = mockk()

  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository)

  private lateinit var testComposeRegisterFragment: TestComposableRegisterFragment

  @Before
  fun setUp() {
    hiltRule.inject()

    testComposeRegisterFragment = TestComposableRegisterFragment()
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.commitNow {
        add(testComposeRegisterFragment, TestComposableRegisterFragment.TAG)
      }
    }
    activityScenarioRule.scenario.moveToState(Lifecycle.State.RESUMED)
  }

  @After
  fun tearDown() {
    activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
  }

  @Test
  fun testFragmentViewSetup() {
    Assert.assertNotNull(testComposeRegisterFragment.view)
    Assert.assertTrue(testComposeRegisterFragment.view is ComposeView)
  }

  @Test
  fun testPerformFilter() {
    Assert.assertTrue(
      testComposeRegisterFragment.performFilter(
        registerFilterType = RegisterFilterType.SEARCH_FILTER,
        data = "2",
        value = 2
      )
    )
    Assert.assertTrue(
      testComposeRegisterFragment.performFilter(
        registerFilterType = RegisterFilterType.OVERDUE_FILTER,
        data = "50",
        value = 51
      )
    )
    Assert.assertFalse(
      testComposeRegisterFragment.performFilter(
        registerFilterType = RegisterFilterType.OVERDUE_FILTER,
        data = "50",
        value = 50
      )
    )
  }

  @Test
  fun testLastSyncTimeStampUpdated() {
    testComposeRegisterFragment.registerViewModel.lastSyncTimestamp.value =
      OffsetDateTime.now().toString()
    Assert.assertNotNull(testComposeRegisterFragment.registerDataViewModel.showLoader.value)
  }

  class TestComposableRegisterFragment : ComposeRegisterFragment<Int, String>() {

    private val testRegisterRepository = TestRegisterRepository()

    override fun navigateToDetails(uniqueIdentifier: String) {
      // Do nothing
    }

    override fun onItemClicked(listenerIntent: ListenerIntent, data: String) {
      // Do nothing
    }

    override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Int, String> =
      RegisterDataViewModel(requireActivity().application, testRegisterRepository)

    override fun performFilter(
      registerFilterType: RegisterFilterType,
      data: String,
      value: Any
    ): Boolean =
      when (registerFilterType) {
        RegisterFilterType.SEARCH_FILTER -> data == value.toString()
        RegisterFilterType.OVERDUE_FILTER -> value.toString().toInt() > 50
      }

    @Composable
    override fun ConstructRegisterList(pagingItems: LazyPagingItems<String>, modifier: Modifier) {
      LazyColumn(modifier = Modifier) {
        items(pagingItems, key = { it }) {
          Text(text = it!!)
          Divider(color = DividerColor, thickness = 1.dp)
        }

        pagingItems.apply {
          when {
            loadState.refresh is LoadState.Loading ->
              item { CircularProgressBar(modifier = Modifier.fillParentMaxSize()) }
            loadState.append is LoadState.Loading -> item { CircularProgressBar() }
            loadState.refresh is LoadState.Error -> {
              val loadStateError = pagingItems.loadState.refresh as LoadState.Error
              item {
                ErrorMessage(
                  message = loadStateError.error.localizedMessage!!,
                  modifier = Modifier.fillParentMaxSize(),
                  onClickRetry = { retry() }
                )
              }
            }
            loadState.append is LoadState.Error -> {
              val error = pagingItems.loadState.append as LoadState.Error
              item {
                ErrorMessage(message = error.error.localizedMessage!!, onClickRetry = { retry() })
              }
            }
          }
        }
      }
    }

    companion object {
      const val TAG = "TestComposableRegisterFragment"
    }
  }

  class TestRegisterRepository : RegisterRepository<Int, String> {

    private val numbers = intArrayOf(100).map { it.toString() }

    override val dataMapper: DataMapper<Int, String> by lazy {
      object : DataMapper<Int, String> {
        override fun transformInputToOutputModel(dto: Int): String = dto.toString()
      }
    }

    override val fhirEngine by lazy { spyk<FhirEngine>() }

    override suspend fun loadData(query: String, pageNumber: Int, loadAll: Boolean) = numbers

    override suspend fun countAll(): Long = numbers.size.toLong()
  }
}
