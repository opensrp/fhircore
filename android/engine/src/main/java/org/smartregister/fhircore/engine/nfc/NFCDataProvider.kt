package org.smartregister.fhircore.engine.nfc

import com.famoco.desfireservicelib.DESFireServiceAccess
import com.github.os72.protobuf.dynamic.DynamicSchema

/**
 * Since activities cannot share the same viewModel, this object is needed to share information
 * between each viewModel of each activities.
 */
object NFCDataProvider {
    //Values that will be shared between each viewModel in order to keep consistency
    var protoFile: DynamicSchema = DynamicSchema.newBuilder().build()
    var fileNumber: String = DESFireServiceAccess.File.RECORD_FILE
}