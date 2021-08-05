package org.smartregister.fhircore.shadow

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.smartregister.fhircore.activity.CovaxDetailActivityTest
import java.text.SimpleDateFormat

object TestUtils {
  private val iParser: IParser = FhirContext.forR4().newJsonParser()

  fun loadQuestionnaire(context: Context, questionnaire: String): Questionnaire {
    val qJson = context.assets.open(questionnaire).bufferedReader().use { it.readText() }
    return iParser.parseResource(qJson) as Questionnaire
  }

  val TEST_PATIENT_1 = Patient().apply {
    id = "test_patient_1_id"
    gender = Enumerations.AdministrativeGender.MALE
    name =
      mutableListOf(
        HumanName().apply {
          addGiven("jane")
          setFamily("Mc")
        }
      )
    telecom = mutableListOf(ContactPoint().apply { value = "12345678" })
    address =
      mutableListOf(
        Address().apply {
          city = "Nairobi"
          country = "Kenya"
        }
      )
    active = true
    birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25")
  }
}
