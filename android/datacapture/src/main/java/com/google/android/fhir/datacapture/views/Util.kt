/*
 * Copyright 2024 Google LLC
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

package com.google.android.fhir.datacapture.views

import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

/** [delay] in milli seconds */
fun TextView.afterTextChangedDelayed(delay: Long, afterTextChanged: (String?) -> Unit) {
  this.addTextChangedListener(
    object : TextWatcher {
      var timer: CountDownTimer? = null
      var firstCharacter: Boolean = true

      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        firstCharacter = p0.isNullOrEmpty()
      }

      override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

      override fun afterTextChanged(editable: Editable?) {
        timer?.cancel()
        // If editable has become empty or this is the first character then invoke afterTextChanged
        // instantly else start a timer for user to finish typing
        if (editable?.toString().isNullOrEmpty() || firstCharacter) {
          afterTextChanged(editable?.toString())
        } else {
          // countDownInterval is simply kept greater than delay as we don't need onTick
          timer =
            object : CountDownTimer(delay, delay * 2) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                  afterTextChanged(editable?.toString())
                }
              }
              .start()
        }
      }
    },
  )
}
