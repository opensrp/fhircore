package org.smartregister.fhircore.engine.cql

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.*

class LibraryEvaluatorTest {
    var libraryEvaluator: LibraryEvaluator? = null
    var libraryData = ""
    var helperData = ""
    var valueSetData = ""
    var testData = ""
    var result = ""
    var evaluatorId = "ANCRecommendationA2"
    var context = "Patient"
    var contextLabel = "mom-with-anemia"
    @Before
    fun setUp() {
        try {
            libraryData = readJsonFile(ASSET_BASE_PATH + "library.json")
            helperData = readJsonFile(ASSET_BASE_PATH + "helper.json")
            valueSetData = readJsonFile(ASSET_BASE_PATH + "valueSet.json")
            testData = readJsonFile(ASSET_BASE_PATH + "patient.json")
            result = readJsonFile(ASSET_BASE_PATH + "result.json")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Test
    fun runCql() {
        libraryEvaluator = LibraryEvaluator()
        val auxResult = libraryEvaluator!!.runCql(
            libraryData,
            helperData,
            valueSetData,
            testData,
            evaluatorId,
            context,
            contextLabel
        )
        Assert.assertEquals(result, auxResult)
    }

    @Throws(IOException::class)
    private fun readJsonFile(filename: String): String {
        val br = BufferedReader(
            InputStreamReader(
                FileInputStream(filename)
            )
        )
        val sb = StringBuilder()
        var line = br.readLine()
        while (line != null) {
            sb.append(line)
            line = br.readLine()
        }
        return sb.toString()
    }

    companion object {
        val ASSET_BASE_PATH = (System.getProperty("user.dir")
                + File.separator + "src" + File.separator
                + "test" + File.separator + "resources" + File.separator)
    }
}