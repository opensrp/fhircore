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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.LoadState
import org.smartregister.fhircore.engine.ui.components.register.DEFAULT_MAX_PAGE_COUNT
import org.smartregister.fhircore.engine.ui.components.register.NoResults
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.components.register.RegisterHeader
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val DEFAULT_MAX_HEIGHT = 0.5f

/**
 * TODO fix issue with ktfmt formatting annotated high order functions. Current workaround below:
 * lambda in this format content: (@Composable() () -> Unit) to allow spotlessApply
 */
@Composable
fun PaginatedRegister(
  loadState: LoadState,
  showResultsCount: Boolean,
  resultCount: Int,
  showHeader: Boolean,
  body: (@Composable() () -> Unit),
  showFooter: Boolean,
  currentPage: Int,
  pagesCount: Int,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier,
  maxHeight: Float = DEFAULT_MAX_HEIGHT
) {
  val bottomPadding = if (showFooter) 48.dp else 0.dp
  ConstraintLayout(modifier = Modifier.fillMaxWidth().fillMaxHeight(maxHeight)) {
    val (bodyRef, searchFooterRef) = createRefs()
    Column(
      modifier =
        modifier.fillMaxSize().constrainAs(bodyRef) {
          height = Dimension.wrapContent
          start.linkTo(parent.start)
          top.linkTo(parent.top)
          end.linkTo(parent.end)
        }
    ) {
      if (showHeader) {
        if (showResultsCount) {
          RegisterHeader(resultCount = resultCount)
        }
      }
      Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = bottomPadding)
      ) {
        if (loadState == LoadState.Loading) {
          CircularProgressBar()
        } else {
          if (resultCount == 0 && showResultsCount) {
            NoResults(modifier = modifier)
          } else {
            body()
          }
        }
      }
    }
    if (showFooter) {
      if (!showResultsCount) {
        Box(modifier = Modifier.constrainAs(searchFooterRef) { bottom.linkTo(parent.bottom) }) {
          RegisterFooter(
            resultCount = resultCount,
            currentPage = currentPage,
            pagesCount = pagesCount,
            previousButtonClickListener = previousButtonClickListener,
            nextButtonClickListener = nextButtonClickListener
          )
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PaginatedRegisterPreviewWithResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = true,
    resultCount = 0,
    showHeader = true,
    body = { Text(text = "Something cool") },
    showFooter = true,
    currentPage = 0,
    pagesCount = DEFAULT_MAX_PAGE_COUNT,
    previousButtonClickListener = {},
    nextButtonClickListener = {},
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PaginatedRegisterPreviewWithoutResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = false,
    resultCount = 0,
    showHeader = true,
    body = { Text(text = "Something cool") },
    showFooter = true,
    currentPage = 0,
    pagesCount = DEFAULT_MAX_PAGE_COUNT,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PaginatedRegisterPreviewWithoutHeaderAndFooter() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = false,
    resultCount = 0,
    showHeader = false,
    body = { Text(text = "Something cool") },
    showFooter = false,
    currentPage = 0,
    pagesCount = DEFAULT_MAX_PAGE_COUNT,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}
