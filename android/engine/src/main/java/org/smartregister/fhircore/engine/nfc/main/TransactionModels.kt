/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.nfc.main

import com.google.gson.Gson

enum class TransactionType(val transactionType: String) {
  ASSISTANCE_VISIT("asv"),
  ANTHROPOMETRIC_VISIT("atv")
}

data class AssistanceVisit(
  val patientId: String,
  val visitNumber: Int,
  val date: String,
  val timestamp: String,
  val rusfAvailable: Boolean,
  val rationType: String,
  val nextVisitDays: Int,
  val nextVisitDate: String,
  val counselType: String,
  val communicationMonitoring: String,
  val exit: Boolean,
  val exitOption: String
)

data class AssistanceVisitNfcModel(val asv: String)

fun getAssistanceVisitData(assistanceItem: AssistanceVisit): String {
  val delimiter = "|"
  return assistanceItem
    .patientId
    .plus(delimiter)
    .plus(assistanceItem.visitNumber)
    .plus(delimiter)
    .plus(assistanceItem.date)
    .plus(delimiter)
    .plus(assistanceItem.timestamp)
    .plus(delimiter)
    .plus(assistanceItem.rusfAvailable)
    .plus(delimiter)
    .plus(assistanceItem.rationType)
    .plus(delimiter)
    .plus(assistanceItem.nextVisitDays)
    .plus(delimiter)
    .plus(assistanceItem.counselType)
    .plus(delimiter)
    .plus(assistanceItem.communicationMonitoring)
    .plus(delimiter)
    .plus(assistanceItem.exit)
    .plus(delimiter)
    .plus(assistanceItem.exitOption)
}

fun getAssistanceVisitQRAnswersToNfcMap(): HashMap<String, String> {
  val assistanceVisitAnswersToNfcMap = hashMapOf<String, String>()
  assistanceVisitAnswersToNfcMap[
    "optimal-dietary-practices-for-adults,-including-pregnant-and-lactating-women"] = "ODP"
  assistanceVisitAnswersToNfcMap["use-of-therapeutic-foods"] = "UTF"
  assistanceVisitAnswersToNfcMap["infant-and-young-child-feeding-(iycf)"] = "IYCF"
  assistanceVisitAnswersToNfcMap["water,-hygiene-and-sanitation-(wash)"] = "WASH"
  assistanceVisitAnswersToNfcMap["arv-adherence"] = "ARV"
  assistanceVisitAnswersToNfcMap["other"] = "other"
  assistanceVisitAnswersToNfcMap["exit-for-transfer-to-sam"] = "ESAM"
  assistanceVisitAnswersToNfcMap["exit-for-transfer-to-other-mam"] = "EMAM"
  assistanceVisitAnswersToNfcMap["exit-cured"] = "EC"
  assistanceVisitAnswersToNfcMap["lost-sight"] = "LS"
  assistanceVisitAnswersToNfcMap["deceased"] = "ED"
  assistanceVisitAnswersToNfcMap["-en-(nutritional-education)"] = "EN"
  assistanceVisitAnswersToNfcMap["dc-(demonstration-culinaire)"] = "DC"
  assistanceVisitAnswersToNfcMap["vad-(household-visit)"] = "VAD"
  assistanceVisitAnswersToNfcMap["field-community-meeting-(reeunion-communautaire)"] = "FCM"

  return assistanceVisitAnswersToNfcMap
}

fun getAssistanceVisitMap(): HashMap<String, Int> {
  val assistanceVisitMap = hashMapOf<String, Int>()
  assistanceVisitMap["patientId"] = 0
  assistanceVisitMap["visitNumber"] = 1
  assistanceVisitMap["date"] = 2
  assistanceVisitMap["timestamp"] = 3
  assistanceVisitMap["rusfAvailable"] = 4
  assistanceVisitMap["rationType"] = 5
  assistanceVisitMap["nextVisitDays"] = 6
  assistanceVisitMap["counselType"] = 7
  assistanceVisitMap["communicationMonitoring"] = 8
  assistanceVisitMap["exit"] = 9
  assistanceVisitMap["exitOption"] = 10

  return assistanceVisitMap
}

fun getAssistanceVisitsFromNfcData(nfcData: String): ArrayList<AssistanceVisit> {
  var assistanceVisitItems: ArrayList<AssistanceVisit> = arrayListOf()

  // Only delimiter we can split the nfc data with is a closing bracket
  // this has to be re-added to the split data to make it a valid json
  val delimiter = "}"
  val transactions = nfcData.split(delimiter)
  for (transaction in transactions) {
    if (transaction.contains(TransactionType.ASSISTANCE_VISIT.transactionType)) {
      val assistanceVisit =
        Gson().fromJson(transaction.plus(delimiter), AssistanceVisitNfcModel::class.java)
      populateAssistanceItem(assistanceVisit)?.let { assistanceVisitItems.add(it) }
    }
  }
  return assistanceVisitItems
}

fun populateAssistanceItem(assistanceVisit: AssistanceVisitNfcModel): AssistanceVisit? {
  val transactionData = assistanceVisit.asv.split("|")
  val assistanceVisitMap = getAssistanceVisitMap()

  try {
    return AssistanceVisit(
      patientId = transactionData[assistanceVisitMap.get("patientId")!!],
      visitNumber = transactionData[assistanceVisitMap.get("visitNumber")!!].toInt(),
      date = transactionData[assistanceVisitMap.get("date")!!],
      timestamp = transactionData[assistanceVisitMap.get("timestamp")!!],
      rusfAvailable = transactionData[assistanceVisitMap.get("rusfAvailable")!!].toBoolean(),
      rationType = transactionData[assistanceVisitMap.get("rationType")!!],
      nextVisitDays = transactionData[assistanceVisitMap.get("nextVisitDays")!!].toInt(),
      nextVisitDate = "2022-12-01",
      counselType = transactionData[assistanceVisitMap.get("counselType")!!],
      communicationMonitoring =
        transactionData[assistanceVisitMap.get("communicationMonitoring")!!],
      exit = transactionData[assistanceVisitMap.get("exit")!!].toBoolean(),
      exitOption = transactionData[assistanceVisitMap.get("exitOption")!!],
    )
  } catch (e: Exception) {
    return null
  }
}
