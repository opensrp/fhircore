package org.smartregister.fhircore.engine.nfc

import android.content.Intent
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.famoco.desfireservicelib.DESFireServiceAccess
import kotlinx.coroutines.Job
import org.smartregister.fhircore.engine.databinding.ActivityMainBinding
import org.smartregister.fhircore.engine.nfc.read.ReadNfcActivity
import org.smartregister.fhircore.engine.nfc.write.WriteNfcActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect

class MainNfcActivity : AppCompatActivity()  {

    private lateinit var binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var eventJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel.connectService(this)

        binding.textViewProto.movementMethod = ScrollingMovementMethod()
        binding.textViewResult.movementMethod = ScrollingMovementMethod()

        //Choose the file we want to perform NFC action
        binding.switchMaterial.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mainViewModel.setFileNumber(DESFireServiceAccess.File.BINARY_FILE)
            } else {
                mainViewModel.setFileNumber(DESFireServiceAccess.File.RECORD_FILE)
            }
        }

        //Generate the proto file depending on the chosen file
        binding.protoBtn.setOnClickListener {
            mainViewModel.generateProtoFile()
        }

        binding.initBtn.setOnClickListener {
            mainViewModel.initSAM()
        }

        //Perform Read action with the UI given by the Service
        binding.readBtn.setOnClickListener {
            mainViewModel.readSerialized()
        }

        //Perform Read action with the UI given by the current application (end-user activity)
        binding.readIntentBtn.setOnClickListener {
            startActivity(Intent(this, ReadNfcActivity::class.java))
        }

        //Perform Write action with the UI given by the Service
        binding.writeBtn.setOnClickListener {
            //hardcoded content that will be written in the card

            //val json = "{\"uint32\": 7,\"uint64\": 1445378,\"float\": 1.5,\"bool\": false,\"string\": \"Service UI rewritten\"}"
            // val json = "{\"resourceType\":\"QuestionnaireResponse\",\"status\":\"in-progress\",\"item\":[{\"linkId\":\"f0361e64-db57-495a-8c82-fa6576e84f74\",\"text\":\"First Name\",\"answer\":[{\"valueString\":\"Micah\"}]},{\"linkId\":\"007eadd1-3929-4786-803a-72df7a11735b\",\"text\":\"Last Name\",\"answer\":[{\"valueString\":\"Berg\"}]},{\"linkId\":\"be2b7976-84a9-47ce-d370-d85eac562c1c\",\"text\":\"Father's Name\",\"answer\":[{\"valueString\":\"Matt\"}]},{\"linkId\":\"216db7ae-08a2-483c-8fdb-a9113c0635fd\",\"text\":\"Date of Birth\",\"answer\":[{\"valueString\":\"12/23/24\"}]},{\"linkId\":\"38896946-7046-42f0-dabd-6ed855965a38\",\"text\":\"Age (in months)\",\"answer\":[{\"valueString\":\"6\"}]},{\"linkId\":\"23e7f371-6996-417e-af8c-6df395ba04e1\",\"text\":\"Gender\"},{\"linkId\":\"cac87acc-6d63-4a57-9dab-34be3dc8ab2a\",\"text\":\"Eligible Child\",\"item\":[{\"linkId\":\"2e4dec25-fa6e-4d5f-bfce-8427f5c7a947\",\"text\":\"Registration ID\",\"answer\":[{\"valueString\":\"123\"}]},{\"linkId\":\"aed720ee-2c0b-4fc5-85d6-a6e8026ade11\",\"text\":\"Beneficiary Group\",\"answer\":[{\"valueCoding\":{\"code\":\"sid-kids-r-us\",\"display\":\"Sid Kids R Us\",\"system\":\"urn:uuid:15d4dda8-e346-4681-e331-91ccdc76a261\"}}]},{\"linkId\":\"1fda3102-d386-487e-bce5-9154bb80a988\",\"text\":\"Caretaker's name\",\"answer\":[{\"valueString\":\"Leigh\"}]},{\"linkId\":\"dca63b9f-c20d-4a78-9cc0-a26d0f4b4f9b\",\"text\":\"Relationship with Child\",\"answer\":[{\"valueCoding\":{\"code\":\"mother\",\"display\":\"Mother\",\"system\":\"urn:uuid:55ef7e98-5fa6-45b4-82b7-a73529ffbc8d\"}}]},{\"linkId\":\"01388f41-7668-4093-c924-3ae43fddfc43\",\"text\":\"Village\",\"answer\":[{\"valueCoding\":{\"code\":\"village-a\",\"display\":\"Village A\",\"system\":\"urn:uuid:0c2434b1-dba8-4f62-d32d-3cb3b1f21713\"}}]},{\"linkId\":\"6147ca2c-68b8-4b8f-85e7-814876858c6d\",\"text\":\"Health Centre\",\"answer\":[{\"valueCoding\":{\"code\":\"hc1\",\"display\":\"HC1\",\"system\":\"urn:uuid:d143f1e3-fe95-4b00-8537-b406777813e0\"}}]}]},{\"linkId\":\"c0416d47-c104-4f70-8c92-79bed7ccf851\",\"text\":\"Not Eligible\",\"item\":[{\"linkId\":\"e88ff534-127b-470a-848b-a341790864df\",\"text\":\"The child is not eligible for CMAM based on their age.\"}]}]}"

            //val json = "{\"string\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxstring1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}"
            val json = "{\"string\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"string1\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"string2\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}"
            //val json = "{}"
            mainViewModel.writeSerialized(json)
        }

        //Perform Write action with the UI given by the current application (end-user activity)
        binding.writeIntentBtn.setOnClickListener {
            startActivity(Intent(this, WriteNfcActivity::class.java))
        }

        mainViewModel.protoFileLiveData.observe(this) {
            val protoString = it?.toString()
            binding.textViewProto.text = protoString
        }

        //Check state connection with the service
        DESFireServiceAccess.DESFireServiceConnectionState.observe(this) {
            binding.textViewConnectionState.text = it.name
        }

        //Check if card interaction status
        DESFireServiceAccess.cardReaderState.observe(this) {
            binding.textViewCardState.text = it.name
        }

        //After reading the card, the end-user will need the content that has been read on the card
        DESFireServiceAccess.readResult.observe(this) { result ->
            if (!result.isNullOrEmpty()) {
                val stringBuilder =
                    StringBuilder().append("Here is the result after reading the card :\n")
                result.forEach {
                    stringBuilder.append(it).append("\n")
                }
                binding.textViewResult.text = stringBuilder.toString()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //Event that prompt only once to be able to know what has just happen with the card reader
        //This consumption will be used if the end-user want to use the Read/Write Activities
        //from the DESFire Service, so that the event can be consume inside the end-user app.
        eventJob = lifecycleScope.launchWhenStarted {
            DESFireServiceAccess.eventFlow.collect { event ->
                Toast.makeText(baseContext, event.name, Toast.LENGTH_SHORT).show()
            }
        }
        eventJob.start()
    }

    override fun onPause() {
        super.onPause()
        //In order to consume the event in ReadNfcActivity or WriteNfcActivity,
        //we need to cancel this eventJob before leaving the MainActivity,
        //because the event will be consumed only once
        eventJob.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.disconnectService(this)
    }
}