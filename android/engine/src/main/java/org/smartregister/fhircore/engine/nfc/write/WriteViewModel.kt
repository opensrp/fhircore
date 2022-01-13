package org.smartregister.fhircore.engine.nfc.write

import androidx.lifecycle.ViewModel
import org.opencds.cqf.cql.engine.data.DataProvider
import org.smartregister.fhircore.engine.nfc.NFCDataProvider

class WriteViewModel: ViewModel() {

    //Set the 3 values needed to interact with the SDK thank to the data saved in the DataProvider object
    val protoFile = NFCDataProvider.protoFile
    val fileNumber = NFCDataProvider.fileNumber
    val jsonDataToWrite = "{\"uint32\": 7,\"uint64\": 1445378,\"float\": 3.5,\"bool\": true,\"string\": \"User UI\"}"
}