package org.smartregister.fhircore.engine.nfc.read

import androidx.lifecycle.ViewModel
import org.smartregister.fhircore.engine.nfc.NFCDataProvider

class ReadViewModel : ViewModel() {

  // Set the 3 values needed to interact with the SDK thank to the data saved in the DataProvider
  // object
  // No need of jsonDataToWrite here because the action is only to read the card
  val protoFile = NFCDataProvider.protoFile
  val fileNumber = NFCDataProvider.fileNumber
  val jsonDataToWrite: String? = null
}
