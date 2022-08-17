package org.smartregister.fhircore.geowidget

import junit.framework.TestCase

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 08-08-2022.
 */
class KujakuConversionInterfaceTest : TestCase() {

    fun testCheckConversionGuide() {}

    fun testFetchConversionGuide() {}

    fun testGenerateFeatureCollection() {
        val locationJson = """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"phone","value":"(+254)0709854000"},{"system":"phone","value":"(+254)0730643000"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388},"managingOrganization":{"reference":"Organization/400"},"partOf":{"reference":"Location/136710"}}"""
        val groupJson = """{"resourceType":"Group","id":"1122f50c-5499-4eaa-bd53-a5364371a2ba","meta":{"versionId":"5","lastUpdated":"2022-06-23T14:55:37.217+00:00","source":"#75f9db2107ef0977"},"identifier":[{"use":"official","value":"124"},{"use":"secondary","value":"c90cd5e3-a1c4-4040-9745-433aea9fe174"}],"active":true,"type":"person","code":{"coding":[{"system":"https://www.snomed.org","code":"35359004","display":"Family"}]},"name":"new family","managingEntity":{"reference":"Organization/105"},"member":[{"entity":{"reference":"Patient/7d84a2d0-8706-485a-85f5-8313f16bafa1"}},{"entity":{"reference":"Patient/0beaa1e3-64a9-436f-91af-36cbdaff5628"}},{"entity":{"reference":"Patient/a9e466a6-6237-46e0-bcda-c66036414aed"}},{"entity":{"reference":"Patient/7e62cc99-d992-484c-ace8-a43dba87ed22"}},{"entity":{"reference":"Patient/cd1c9616-bdfd-4947-907a-5f08e2bcd8a9"}}]}"""


        val conversionInterface = KujakuConversionInterface()

        //conversionInterface.fetchConversionGuide(c)

    }
}