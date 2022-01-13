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
import org.opencds.cqf.cql.engine.data.DataProvider

class MainViewModel : ViewModel()  {
    //LiveData that will be updated depending on the user interaction in the activity
    private val _protoFile =
        MutableLiveData<DynamicSchema>().apply { value = NFCDataProvider.protoFile }
    val protoFileLiveData: LiveData<DynamicSchema> = _protoFile

    private val _fileNumber = MutableLiveData<String>().apply { value = NFCDataProvider.fileNumber }

    fun setFileNumber(file: String) {
        _fileNumber.postValue(file)
        NFCDataProvider.fileNumber = file
    }

    //Depending on what file is selected, a different proto file is generated (and so used)
    fun generateProtoFile() {
        when (_fileNumber.value) {
            DESFireServiceAccess.File.BINARY_FILE -> generateProtoFileBinaryFile()
            DESFireServiceAccess.File.RECORD_FILE -> generateProtoFileRecordFile()
        }
    }

    /**
     * Method used to generate a proto file, that will be pass as a ByteArray to the SDK in order to perform NFC action
     *
     */
    private fun generateProtoFileBinaryFile() {
        val dynamicSchema = DynamicSchema.newBuilder()
        dynamicSchema.setName("binary.proto")
        dynamicSchema.setPackage("famoco")
        val messageBuilder = MessageDefinition.newBuilder("Event")
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.UINT32, "uint32", 1)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.UINT64, "uint64", 2)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.FLOAT, "float", 3)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.BOOL, "bool", 4)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string", 5)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string1", 6)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string2", 7)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string3", 8)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string4", 9)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string5", 10)
        )
        //Add here each line to add in the desc file
        //the last parameter is th id that must be different for each field
        dynamicSchema.addMessageDefinition(messageBuilder.build());
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
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.UINT32, "uint32", 1)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.UINT64, "uint64", 2)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.FLOAT, "float", 3)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.BOOL, "bool", 4)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string", 5)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string1", 6)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string2", 7)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string3", 8)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string4", 9)
        )
        addProtobufField(
            messageBuilder,
            ProtobufFieldDesc(null, ProtobufFieldDesc.ProtobufType.STRING, "string5", 10)
        )
        //Add here each line to add in the desc file
        //the last parameter is th id that must be different for each field
        dynamicSchema.addMessageDefinition(messageBuilder.build());
        try {
            val dyn = dynamicSchema.build()
            Log.d("TAG", ".proto FILE -> \n $dyn")
            _protoFile.postValue(dyn)
            NFCDataProvider.protoFile = dyn
        } catch (e: Descriptors.DescriptorValidationException) {
            throw UnexpectedTechnicalException(
                "Protobuf description must be valid at this point => check code",
                e
            );
        }
    }

    private fun addProtobufField(
        messageBuilder: MessageDefinition.Builder,
        protobufFieldDesc: ProtobufFieldDesc
    ) {
        protobufFieldDesc.newCopy()
            .fieldLabel(ProtobufFieldDesc.ProtobufLabel.OPTIONAL)
            .addInMessageDefinition(messageBuilder)
    }

    //Methods to communicate with the library

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