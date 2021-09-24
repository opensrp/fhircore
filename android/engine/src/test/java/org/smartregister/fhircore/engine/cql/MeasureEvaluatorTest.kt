package org.smartregister.fhircore.engine.cql

import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil
import java.io.File

class MeasureEvaluatorTest {

    var baseTestPathMeasureAssets=System.getProperty("user.dir")+ File.separator + "src" + File.separator+File.separator+"test/resources/cql/measureevaluator/"
    var patientAssetsDir=baseTestPathMeasureAssets+"first-contact"
    var libraryFilePath="test/resources/cql/measureevaluator/library/ANCIND01-bundle.json"

    @Test
    fun runMeasureEvaluate() {
        var filePatientAssetDir= File(patientAssetsDir)
        var fileUtil=FileUtil()
        var fileListString=fileUtil.recurse(filePatientAssetDir)
        var patientResources: ArrayList<String> = ArrayList()

        for (f in fileListString){
            patientResources.add(fileUtil.readJsonFile(f))
        }

        var measureEvaluator=MeasureEvaluator()

        measureEvaluator.runMeasureEvaluate(
            fileUtil.readJsonFile(libraryFilePath),
            patientResources
        )
    }
}