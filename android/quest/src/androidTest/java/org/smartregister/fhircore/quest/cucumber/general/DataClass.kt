package org.smartregister.fhircore.quest.cucumber.general

import java.io.File


class DataClass {
    var summary: String? = null
    var Description: String? = null
    var Method: String? = null
    var methodName: String? = null
    var csvRow: String? = null
    var imageFile: File? = null
    var automationSteps: ArrayList<String>? = null
    var lengthBeforeTestCase: Int? = null
    var lengthAfterTestCase: Int? = null
}