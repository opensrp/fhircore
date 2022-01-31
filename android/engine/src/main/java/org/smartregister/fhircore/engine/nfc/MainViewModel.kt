package org.smartregister.fhircore.engine.nfc

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.famoco.desfireservicelib.DESFireServiceAccess
import com.famoco.desfireservicelib.utils.proto.ProtobufFieldDesc
import com.famoco.desfireservicelib.utils.proto.UnexpectedTechnicalException
import com.github.os72.protobuf.dynamic.DynamicSchema
import com.github.os72.protobuf.dynamic.MessageDefinition
import com.google.protobuf.Descriptors

class MainViewModel : ViewModel() {
  // LiveData that will be updated depending on the user interaction in the activity
  private val _protoFile =
    MutableLiveData<DynamicSchema>().apply { value = NFCDataProvider.protoFile }
  val protoFileLiveData: LiveData<DynamicSchema> = _protoFile

  private val _fileNumber = MutableLiveData<String>().apply { value = NFCDataProvider.fileNumber }

  fun setFileNumber(file: String) {
    _fileNumber.postValue(file)
    NFCDataProvider.fileNumber = file
  }

  // Depending on what file is selected, a different proto file is generated (and so used)
  fun generateProtoFile() {
    when (_fileNumber.value) {
      DESFireServiceAccess.File.BINARY_FILE -> generateProtoFileBinaryFile()
      DESFireServiceAccess.File.RECORD_FILE -> generateProtoFileRecordFile()
    }
  }

  /**
   * Method used to generate a proto file, that will be pass as a ByteArray to the SDK in order to
   * perform NFC action
   */
  private fun generateProtoFileBinaryFile() {
    val dynamicSchema = DynamicSchema.newBuilder()
    dynamicSchema.setName("binary.proto")
    dynamicSchema.setPackage("famoco")
    val messageBuilder = MessageDefinition.newBuilder("Event")

    // TODO build this dynamically
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "patient_id", 1)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "first_name", 2)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "last_name", 3)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "middle_name", 4)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "age", 5)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "birth_date", 6)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "gender", 8)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "caretaker_name", 9)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "caretaker_relationship", 10)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "village", 11)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "health_center", 12)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "beneficiary_group", 13)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "registration_date", 14)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "creation_date", 15)
    )
    // Add here each line to add in the desc file
    // the last parameter is th id that must be different for each field
    dynamicSchema.addMessageDefinition(messageBuilder.build())
    try {
      val dyn = dynamicSchema.build()
      Log.d("TAG", ".proto FILE -> \n $dyn")
      _protoFile.postValue(dyn)
      NFCDataProvider.protoFile = dyn
    } catch (e: Descriptors.DescriptorValidationException) {
      throw UnexpectedTechnicalException(
        "Protobuf description must be valid at this point => check code",
        e
      )
    }
  }

  private fun generateProtoFileRecordFile() {
    val dynamicSchema = DynamicSchema.newBuilder()
    dynamicSchema.setName("record.proto")
    dynamicSchema.setPackage("famoco")
    val messageBuilder = MessageDefinition.newBuilder("Event")

    // TODO build this dynamically
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "asv", 1)
    )
    addProtobufField(
      messageBuilder,
      ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "atv", 2)
    )

    // Add here each line to add in the desc file
    // the last parameter is th id that must be different for each field
    dynamicSchema.addMessageDefinition(messageBuilder.build())
    try {
      val dyn = dynamicSchema.build()
      Log.d("TAG", ".proto FILE -> \n $dyn")
      _protoFile.postValue(dyn)
      NFCDataProvider.protoFile = dyn
    } catch (e: Descriptors.DescriptorValidationException) {
      throw UnexpectedTechnicalException(
        "Protobuf description must be valid at this point => check code",
        e
      )
    }
  }

  private fun addProtobufField(
    messageBuilder: MessageDefinition.Builder,
    protobufFieldDesc: ProtobufFieldDesc
  ) {
    protobufFieldDesc
      .newCopy()
      .fieldLabel(ProtobufFieldDesc.ProtobufLabel.OPTIONAL)
      .addInMessageDefinition(messageBuilder)
  }

  // Methods to communicate with the library

  fun connectService(context: Context) {
    DESFireServiceAccess.connect(context)
  }

  fun disconnectService(context: Context) {
    DESFireServiceAccess.disconnect(context)
  }

  fun initSAM() {
    DESFireServiceAccess.init()
  }

  fun readSerialized() {
    val protoFile = _protoFile.value
    val fileNb = _fileNumber.value
    if (null != protoFile && null != fileNb) {
      DESFireServiceAccess.readSerialized(protoFile.toByteArray(), fileNb)
    }
  }

  fun writeSerialized(json: String) {
    val protoFile = _protoFile.value
    val fileNb = _fileNumber.value
    if (null != protoFile && null != fileNb) {
      DESFireServiceAccess.writeSerialized(protoFile.toByteArray(), json, fileNb)
    }
  }
}
