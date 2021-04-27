/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.smartregister.fhircore.R
import org.smartregister.fhircore.fragment.PatientListFragment

/** An activity representing a list of Patients. */
class PatientListActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("PatientListActivity", "onCreate() called")
    setContentView(R.layout.activity_patient_list)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    setUpViews()
  }

  private fun setUpViews() {
    findViewById<Button>(R.id.btn_register_new_patient).setOnClickListener { addPatient(it) }

    findViewById<ViewPager2>(R.id.patient_list_pager).adapter = PatientListPagerAdapter(this)

    var editText = findViewById<EditText>(R.id.edit_text_search)
    editText.doAfterTextChanged {
      if (it!!.isEmpty()) {
        editText.setOnTouchListener(null)
        editText.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_search), null, null, null)
      } else {
        editText.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_back_arrow), null, getDrawable(R.drawable.ic_cancel), null)
        editText.addOnDrawableClickedListener(DrawablePosition.DRAWABLE_LEFT) {}
        editText.addOnDrawableClickedListener(DrawablePosition.DRAWABLE_RIGHT) { it.clear() }
      }
    }

    setupDrawerContent()
  }

  fun EditText.addOnDrawableClickedListener(drawablePosition: DrawablePosition, onClicked: ()-> Unit) {
      this.setOnTouchListener(
        object : View.OnTouchListener {

          override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event!!.action == MotionEvent.ACTION_UP && isDrawableClicked(drawablePosition, event, v as EditText)) {
                onClicked()
                return true;
            }
            return false;
          }
        }
      )
  }

  private fun isDrawableClicked(drawablePosition: DrawablePosition, event: MotionEvent?, view: EditText): Boolean {
    return when (drawablePosition) {
      DrawablePosition.DRAWABLE_RIGHT -> event!!.rawX >= (view.right - view.compoundDrawables[drawablePosition.position].bounds.width())
      DrawablePosition.DRAWABLE_LEFT -> event!!.rawX <= (view.compoundDrawables[drawablePosition.position].bounds.width())
      else -> {
        return false
      }
    }
  }

  enum class DrawablePosition (val position: Int) {
    DRAWABLE_LEFT(0),
    DRAWABLE_TOP(1),
    DRAWABLE_RIGHT(2),
    DRAWABLE_BOTTOM(3)
  }

  private fun setupDrawerContent() {
    val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
    findViewById<ImageButton>(R.id.btn_drawer_menu).setOnClickListener {
      drawerLayout.openDrawer(GravityCompat.START)
    }
  }

  private fun addPatient(view: View) {
    // TO DO: Open patient registration form
    val context = view.context
    context.startActivity(
      Intent(context, QuestionnaireActivity::class.java).apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "patient-registration.json")
      }
    )
  }

  // pager adapter
  private inner class PatientListPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
      return 50
    }

    override fun createFragment(position: Int): Fragment {
      return PatientListFragment()
    }
  }
}
