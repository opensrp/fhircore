package org.smartregister.fhircore.engine.nfc.write

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.famoco.desfireservicelib.CardReaderEvent
import com.famoco.desfireservicelib.DESFireServiceAccess
import com.famoco.desfireservicelib.NFCActivity
import com.famoco.desfireservicelib.NfcMode
import com.famoco.famocodialog.DialogType
import com.famoco.famocodialog.FamocoDialog
import kotlinx.coroutines.flow.collect
import org.smartregister.fhircore.engine.R

//This activity inherit NFC Activity, that is a non-UI Activity and contains all the NFC implementation
class WriteNfcActivity : NFCActivity(){

    private val writeViewModel: WriteViewModel by viewModels()

    private lateinit var dialog: FamocoDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Listening to the event Flow that will be consumed only once, in order to perform some change in the UI
        //Here we will show some SUCCESS or ERROR Dialog depending on the result
        lifecycleScope.launchWhenStarted {
            DESFireServiceAccess.eventFlow.collect { event ->
                if (event == CardReaderEvent.WRITE_SUCCESS) {
                    //Dialog OK
                    dialog = FamocoDialog(this@WriteNfcActivity)
                        .setDialogType(DialogType.SUCCESS)
                        .setTitle(getString(R.string.success))
                        .cancelable(true)
                        .setContent(getString(R.string.write_success_content))
                        .showPositiveButton(false)
                        .setOnNegativeButtonClicked(getString(R.string.close)) {
                            dialog.dismiss()
                            finish()
                        }
                    dialog.show()
                }
                if (event.name.contains("ERROR")) {
                    //Dialog NOK
                    dialog = FamocoDialog(this@WriteNfcActivity)
                        .setDialogType(DialogType.ERROR)
                        .setTitle(getString(R.string.error))
                        .cancelable(true)
                        .setContent(String.format(
                            getString(R.string.write_error_content_format),
                            event.name,
                            event.code
                        ))
                        .showPositiveButton(false)
                        .setOnNegativeButtonClicked(getString(R.string.close)) {
                            dialog.dismiss()
                            finish()
                        }
                    dialog.show()
                }
            }
        }
    }

    /**
     * Methods get from the abstract NFCActivity present in the library
     *
     * @return The end-user layout here that will be used for the current activity
     */
    override fun getLayoutResId(): Int {
        return R.layout.activity_write_nfc
    }


    /**
     * You have to specify the NfcMode you want for this activity
     *
     * @return NfcMode.READ or NfcMode.WRITE to specify the current activity mode
     */
    override fun getNfcMode(): NfcMode {
        return NfcMode.WRITE
    }

    /**
     * You need also to pass to this activity the proto file that will be used in order
     * to perform NFC actions
     * Here the value is already generated in the MainActivity,
     * so we are getting it from the ViewModel that obtain it from the DataProvider object
     *
     * @return the proto file that will be used
     */
    override fun getProtoFile(): ByteArray {
        return writeViewModel.protoFile.toByteArray()
    }

    /**
     * Here the value is already generated in the MainActivity,
     * so we are getting it from the WriteViewModel that obtain it from the DataProvider object
     *
     * @return The file Number on the card to interact with
     */
    override fun getFileNumber(): String {
        return writeViewModel.fileNumber
    }

    /**
     * For the NfcMode.WRITE, you will need to pass the data to write in the card
     * Here the value is already generated in the MainActivity,
     * so we are getting it from the WriteViewModel that obtain it from the DataProvider object
     *
     * @return the data that will be written in the card (if needed)
     */
    override fun getJsonDataToWrite(): String? {
        return writeViewModel.jsonDataToWrite
    }
}