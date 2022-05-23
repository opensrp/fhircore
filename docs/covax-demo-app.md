# Purpose of this document

This document provides end-user instructions on using the OpenSRP Covax Demo app. Additionally, the goal of this document is to lay out the core workflows and related content for our Covax Demo app product for WHO as a reference app that can be piloted or rolled out for an in-country or facility rollout/use case.  


# Acronyms


<table>
  <tr>
   <td><strong>Acronym</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td><strong>API</strong>
   </td>
   <td>Application Programming Interface 
   </td>
  </tr>
  <tr>
   <td><strong>APK</strong>
   </td>
   <td>Android Package is the Android application package file format used by the Android operating system, and several other Android-based operating systems for distribution and installation of mobile apps, mobile games and middleware. It can be written in either Java or Kotlin
   </td>
  </tr>
  <tr>
   <td><strong>COVID-19</strong>
   </td>
   <td>CoronaVirus disease 2019
   </td>
  </tr>
  <tr>
   <td><strong>DDCC</strong>
   </td>
   <td>Digital Documentation of COVID-19 Certificate(s) (DDCC): A digitally signed FHIR document that represents the core data set for the relevant COVID-19 certificate using the JavaScript Object Notation (JSON) representation. 
   </td>
  </tr>
  <tr>
   <td><strong>DDCC: VS</strong>
   </td>
   <td>Digital Documentation of COVID-19 Certificate(s): Vaccination Status. Digital Documentation of COVID-19 Certificates, or DDCC: VS, is a digitally signed representation of data content that describes a vaccination event. DDCC: VS data content respects the specified core data set and follows the Health Level Seven (HL7) Fast Healthcare Interoperability Resources (FHIR) standard detailed in the FHIR Implementation Guide.
<p>
<a href="https://www.who.int/publications/i/item/WHO-2019-nCoV-Digital_certificates-vaccination-data_dictionary-2021.1">https://www.who.int/publications/i/item/WHO-2019-nCoV-Digital_certificates-vaccination-data_dictionary-2021.1</a> 
   </td>
  </tr>
  <tr>
   <td><strong>EIR</strong>
   </td>
   <td>Electronic Immunization Registry
   </td>
  </tr>
  <tr>
   <td><strong>FHIR</strong>
   </td>
   <td>FHIR (Fast Healthcare Interoperability Resources) Specification, is a standard for exchanging healthcare information electronically which could be written in JSON format 
   </td>
  </tr>
  <tr>
   <td><strong>HCID</strong>
   </td>
   <td>Health certificate identifier (HCID) barcode is preprinted on a paper card that is used to store a patient's Immunization details  
   </td>
  </tr>
  <tr>
   <td><strong>JSON</strong>
   </td>
   <td>JavaScript Object Notation
   </td>
  </tr>
  <tr>
   <td><strong>QA</strong>
   </td>
   <td>Quality Assurance 
   </td>
  </tr>
  <tr>
   <td><strong>SNOMED CT GPS</strong>
   </td>
   <td>Systematized Nomenclature of Medicine Clinical Terms Global Patient Set
   </td>
  </tr>
</table>



# Definitions/Glossary 


<table>
  <tr>
   <td><strong>Term</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td><strong>Certificate</strong>
   </td>
   <td>Certificate: A document attesting a fact. In the context of the vaccination certificate, it attests to the fact that a vaccine has been administered to an individual. 
   </td>
  </tr>
  <tr>
   <td><strong>Covax</strong>
   </td>
   <td>COVAX: The vaccines pillar of the Access to COVID-19 Tools (ACT) Accelerator. It aims to accelerate the development and manufacture of COVID-19 vaccines and to guarantee fair and equitable access for every country in the world. 
   </td>
  </tr>
  <tr>
   <td><strong>DDCC: VS Generation service </strong>
   </td>
   <td>The service that is responsible for generating a digitally signed representation, the DDCC, of the information concerning a COVID-19 vaccination.
   </td>
  </tr>
  <tr>
   <td><strong>DDCC: VS Registry Service </strong>
   </td>
   <td>The service can be used to request and receive the digitally signed COVID-19 vaccination information. 
   </td>
  </tr>
  <tr>
   <td><strong>DDCC: VS Repository Service </strong>
   </td>
   <td>A potentially federated service that has a repository, or database, of DDCC: VS. 
   </td>
  </tr>
  <tr>
   <td><strong>HAPI-FHIR Server </strong>
   </td>
   <td>HAPI-FHIR is a Java software library, facilitating a built-in mechanism for adding FHIR's RESTful Server functionalities to a software application. The HAPI FHIR Java library is open source. The HAPI RESTful (Representation State Transfer) Server is based on a Servlet, so it should be deployed with ease to any compliant containers that can be provided. 
   </td>
  </tr>
  <tr>
   <td><strong>Identifier</strong>
   </td>
   <td>A name that labels the identity of an object or individual. Usually, it is a unique alphanumeric string that is associated with an individual, for example, a passport number or medical record ID.
   </td>
  </tr>
  <tr>
   <td><strong>One Dimensional Barcode (1D)</strong>
   </td>
   <td>A visual black and white pattern using variable-width lines and spaces for encoding information in a machine-readable form. It is also known as a linear code. 
   </td>
  </tr>
  <tr>
   <td><strong>Two Dimensional Barcode (2D)</strong>
   </td>
   <td>A two-dimensional barcode (2-D barcode) provides information storage on both horizontal and vertical axes. This graphic image can be printed, embedded on a digital screen or otherwise presented for scanning and analysis
   </td>
  </tr>
  <tr>
   <td><strong>QR Code</strong>
   </td>
   <td>A quick response code (QR code) is a type of two-dimensional bar code that consists of square black modules on a white background. QR codes are designed to be read by smartphones. Because they can carry information both vertically and horizontally, they can provide a vast amount of information, including links, text or other data.
   </td>
  </tr>
  <tr>
   <td><strong>RESTful API</strong>
   </td>
   <td>A REST API (also known as RESTful API) is an application programming interface (API or web API) that conforms to the constraints of REST architectural style and allows for interaction with RESTful web services.
   </td>
  </tr>
</table>


**Github and Patient Administration Web portal Links**: 



1. FHIR Core Repo - [https://github.com/opensrp/fhircore](https://github.com/opensrp/fhircore) 
2. FHIR Core applications release page  - [https://github.com/opensrp/fhircore/releases](https://github.com/opensrp/fhircore/releases) 
3. FHIR Web Administration portal   - [https://fhir-web.opensrp-stage.smartregister.org](https://fhir-web.opensrp-stage.smartregister.org/) 
4. HAPI FHIR server -  [https://fhir.labs.smartregister.org/fhir/](https://fhir.labs.smartregister.org/fhir/) 


#Chapter 1: Overview and Definitions


## 1.1 About this guide 

This is intended to be an end-user guide or user manual and not a Technical Implementation Guide.


## 1.2 Background

Below is a summary of the business requirements in section 1.2.1  translated into key interactions within the application. 

**_NB. Work on the Implementation Guide is not yet complete/shared. _**


###  1.2.1 Desired application  workflow and initial  requirements



1. **User Authentication**  -Ability to authenticate users to login into the app. On successful authentication, we should receive the practitioner’s ID, and the facility ID and location ID of where they are conducting the Immunization drive 
2. **Sync Data**  - Push data to the back end This data can be accessed via an API to the HAPI FHIR server to a 3rd party system that can either generate an SVC or perform other operations to that data 
3. **Patient List**  - View a list of enrolled clients post successful sync from HAPI FHIR server with their status as either
    1. Record Vaccine
    2. 1st Dose 
    3. Fully Vaccinated
4. **Patient Registration** - Ability to register a new patient 
    4. Register a person in the application. It may not need to be resource mapper dependent. Can be a questionnaire response 
    5. Users can be enrolled to the platform for the first time if they are not found  upon searching the system DB or scanning a Barcode with HCID available on their paper card 
    6. Add a barcode reader to query the user’s details. This barcode will contain the composite ID for the patient. The app will generate Composite IDs which essentially can be a bundle with the ID of the patient, demographic details, Vaccines received or even adverse reactions etc. . The bare code can also be encrypted using a Hash.  This can be published on your health card to track you on your vaccination 
5. **Edit Patient Details**  - Edit patient details and make sure the same change is reflected  in HAPI 
6. **Patient search** - Ability to search from a list of patients (eg. pre-enrolled) in an FHIR datastore on the device or from the HAPI FHIR Server. Not mandatory 
7. **Filter Overdue** - Filter all overdue clients who have missed their appointment for the 2nd dose of the shot
8. **Vaccine Registration** - Next, you can record vaccines received via a simple questionnaire and once done you can save the data as a FHIR Immunization resource. It will take the dual dose approach basically 1st Dose, 2nd Dose & Fully vaccinated. Can be a questionnaire response to be submitted.  The vaccine list and Codes are available below

# Vaccine Codes


1. Pfizer–BioNTech  - https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f667112200 
    1. Code  - XM8NQ0 Comirnaty®Pfizer BioNTech - Comirnaty
    2. Type - XM0GQ8 COVID-19 vaccine, RNA based
    3. System - https://icd.who.int/dev11/l-m/en 
    4. Manufacturer - dependent on country  - BioNTech

2. Moderna   - https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f1211296175 
    1. Code  - XM3DT5 COVID-19 Vaccine Moderna
    3. Type - XM0GQ8 COVID-19 vaccine, RNA based
    8. System - https://icd.who.int/dev11/l-m/en 
    10. Manufacturer - dependent on country  - Moderna
    
4. AstraZeneca   - https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f473439328 
    1. Code  - XM4YL8 COVID-19 Vaccine AstraZeneca
    12. Type - XM9QW8 COVID-19 vaccine, non-replicating viral vector
    15. System - https://icd.who.int/dev11/l-m/en 
    16. Manufacturer - dependent on country  - Oxford
    
5. Janssen   - https://icd.who.int/dev11/l-m/en#/http://id.who.int/icd/entity/1838464611 
    1. Code  - XM6QV1 COVID-19 Vaccine Janssen
    7. Type - XM9QW8 COVID-19 vaccine, non-replicating viral vector
    8. System - https://icd.who.int/dev11/l-m/en 
    9. Manufacturer - dependent on country  - Johnson & Johnson 

**For QA**
1. Validate 1st dose is the same as 2nd dose - A user cannot receive another brand of the vaccine different from the 1st dose in a Dual dose immunization
3. Test the Create / Update Immunization resource
4. Make sure Janssen is a single shot vaccine for JnJ (optional) - Not implemented  as all Vaccines are assumed to be Dual dosage shots



## 1.3 DDCC: VS implementation

Digital Documentation of COVID-19 Certificates (DDCC) is proposed as a mechanism by which a person’s COVID-19-related health data can be digitally documented via an electronic certificate. A digital vaccination certificate that documents a person’s current vaccination status to protect against COVID-19 can then be used for continuity of care or as proof of vaccination for purposes other than health care. The resulting artefact of this approach is referred to as the Digital Documentation of COVID-19 Certificates: Vaccination Status (DDCC: VS). A vaccination certificate is a health document that records a vaccination service received by an individual, traditionally as a paper card noting key details about the vaccinated individual, vaccine administered, date administered, and other data in the core data set. i.e. 



1. Name 
3. Date of Birth
4. Identifier 
7. Gender/Sex

Digital vaccination certificates are immunization records in an electronic format that are accessible by both the vaccinated person and authorized health workers, and which can be used in the same way as the paper card: to ensure continuity of care or provide proof of vaccination. These are the two scenarios considered in this document. Some possible uses of DDCC: VS 



1. Continuity of Care
    1. Provides a basis for health workers to offer a subsequent dose and/or appropriate health services 
    2. Provides schedule information for an individual to know whether another dose, and of which vaccine, is needed, and when the next dose is due 
    3. Enables investigation into adverse events by health workers, as per existing guidance on adverse events following immunization (AEFI) (vaccine safety) 
2.  Proof of Vaccination 
    5. Establishes the vaccination status of individuals in coverage monitoring surveys 
    8. Establishes vaccination status after a positive COVID-19 test, to understand vaccine effectiveness For work 
    10. For university education 
    11.  For international travel*

A vaccination certificate can be purely digital (e.g. stored in a smartphone application or on a cloud-based server) and replace the need for a paper card, or it can be a digital representation of the traditional paper-based record. A digital certificate should never require individuals to have a smartphone or computer. The link between the paper record and the digital record can be established using a one-dimensional (1D) or two-dimensional (2D) barcode, for example, printed on or affixed to the paper vaccination card. References to the “paper” record in this document mean a physical document (printed on paper, plastic card, cardboard, etc.). An illustrative example of a DDCC: VS is given in Figure 1. 

For this to work with a DDCC: VS service we have the following set of information / minimum set of requirements 



1. An individual who has received vaccination should have access to proof of this – either as a traditional paper card or a version of the electronic DDCC: VS.
2. Where a paper vaccination card is used, it should be associated with a health certificate identifier (HCID). 
3. A DDCC: VS should be associated, as a digital representation, with the paper vaccination card via the HCID. Multiple forms of digital representations of the DDCC: VS may be associated with the paper vaccination card via the HCID.
4. The HCID should appear on any paper card in both a human-readable and a machine-readable format (i.e. alphanumeric characters that are printed, as well as rendered as a 1D or 2D barcode).

More information can be found here: 	[https://www.who.int/publications/i/item/WHO-2019-nCoV-Digital_certificates-vaccination-data_dictionary-2021.1](https://www.who.int/publications/i/item/WHO-2019-nCoV-Digital_certificates-vaccination-data_dictionary-2021.1) 



<img src="https://user-images.githubusercontent.com/4540684/164892522-045197cb-8c36-4704-99b7-cab0cc67a4d7.png" width="400" height="800" />


Figure 1. Digital Documentation of COVID-19 Certificates: Vaccination Status

Figure 2 and Figure 3 represent the Sequence diagram depicting how data is captured using the reference application and then passed onto the Smart Vaccine Certificate Generation service for getting the  Digital certificate that is Digitally signed for verification purposes and data encoded onto a 2D barcode QR code to be used either as a digital scan or printed onto a Smart Vaccine Certificate paper card. 



![fhir_workflows](https://user-images.githubusercontent.com/4540684/164892607-ef499f53-13cd-4064-9487-02c7d3d1c60e.png)




Figure 2. Smart Vaccine Generation workflow  - Sequence Diagram 


![ri_workflows](https://user-images.githubusercontent.com/4540684/164892618-cda9130b-3d4a-48ec-894a-c81d3d6de857.png)





Figure 3. Smart Vaccine Generation alternative workflow  - Sequence Diagram  


#Chapter 2 : Program Overview


## 2.1 Platform

The WHO Covax demo app is based on the OpenSRP FHIR Core. This next frontier FHIR based application is a Kotlin application for delivering offline-capable, mobile-first healthcare project implementations from local community to national and international scale using [FHIR](https://hapifhir.io/) and the WHO Smart Guidelines on Android.

FHIR Core is architected as a FHIR native digital health platform powered by Google's [Android FHIR SDK](https://github.com/google/android-fhir) and [HAPI FHIR](https://hapifhir.io/). FHIR Core's user experience and module oriented design are based on over a decade of real-world experience implementing digital health projects with [OpenSRP](https://smartregister.org/). This repository contains the Android mobile application built to:



1. load configuration data as FHIR resources,
2. support the WHO Smart Guidelines,
3. manage the identities of healthcare workers (HCWs), community health workers (CHWs), care teams, patients, and clients,
4. collect, view, and edit healthcare data with dynamic forms using FHIR's [Structured Data Capture (SDC)](https://hl7.org/fhir/us/sdc/index.html) implementation,
6. securely store healthcare data encrypted at rest and securely transmit healthcare data using TLS,
7. manage location hierarchies defined by the community to national and international administrative boundaries.

For remote data storage and login, the mobile application requires:

* a [Keycloak](https://www.keycloak.org/) server to manage identity, authentication, and authorization;
* a [HAPI FHIR](https://hapifhir.io/) server to store operation and configuration data that includes the [HAPI FHIR to Keycloak integration](https://github.com/opensrp/hapi-fhir-keycloak).

FHIRcore also interoperates well with OpenSRP Web to access healthcare data from the same HAPI FHIR server. More about the architecture in the docs linked here	  [https://github.com/opensrp/fhircore/tree/main/docs](https://github.com/opensrp/fhircore/tree/main/docs) 


### 2.1.1 Application

The Covax Demo app can be found on the release page of the FHIR Core Github repository link here - [https://github.com/opensrp/fhircore/releases](https://github.com/opensrp/fhircore/releases)  together with a summary of the release notes.


#Chapter 3 - Getting Started


## 3.1 Starting the App

Once you have downloaded the APK from the [releases page](https://github.com/opensrp/fhircore/releases), install it onto your compatible android device and click on the application icon labelled as, FHIR EIR app, to launch the Covax Demo app.  


## 3.2 Login

You will be provided with a unique username and password to log in to the app. Your username and password are unique to you, so you should not share your login details with anyone else. Memorize your password, so that you do not have to write it down. If you forget your username and/or password, contact the support team right away, so that they can help you log back into the app.

On the login page, post launching the app, enter your username and password on the login screen (Figure 4), then tap on the LOGIN button. Check the Show password checkbox to reveal your password. (Figure 5)



<img src="https://user-images.githubusercontent.com/4540684/164892641-36e34de3-f978-4768-a81a-aa88d9b8b7f6.png" width="200" height="400" />
       

Figure 4. Login                       

   

<img src="https://user-images.githubusercontent.com/4540684/164892647-63917a34-7f09-4b70-8a81-a503287b12a1.png" width="200" height="400" />


Figure 5. Authenticate Use


## 3.3  Sync Data 

On successful login, all related app configs and patient data (i.e FHIR Resources - Patient, Immunization records) will be synced to the application. A user will get a prompt when the process begins and upon successful completion of the sync process. In the case of intermittent connectivity, the process will resume as in case of loss of connectivity the user will be prompted with a notification of the same. Once done the user will be able to browse through a paginated patient list view with status to either:



1. Record Vaccine (No dose given) - Navy blue
2. Vaccine Dose 1  - Date (Got 1st jab, awaiting dose 2, not overdue yet) - Grey
3. Overdue (Missed second appointment for the next dose) - Red
4. Vaccinate (All doses received) - Green 


<img src="https://user-images.githubusercontent.com/4540684/164892656-9f8018af-a805-47fa-b952-a2352084b216.png" width="200" height="400" />


Figure 6. Sync In progress



<img src="https://user-images.githubusercontent.com/4540684/164892659-03603f4a-8f4a-4c32-8a32-55d075399278.png" width="200" height="400" />


Figure 7. Sync Complete


## 3.4  Language Selection

The application currently supports Multi-Language support in FHIR (Localization/Internationalization). More details here [https://www.hl7.org/fhir/languages.html](https://www.hl7.org/fhir/languages.html) 

Every resource has an optional [language](https://www.hl7.org/fhir/resource-definitions.html#Resource.language) element that is the base language for the content of the resource. Note that this does not require that all the content of the resource be in the specified language; just that unless otherwise specified, this is the default language of the content. Currently, the app supports language translation from English (en) to Swahili (sw). An example of FHIR Translated Registration Questionnaire snippet is shown below :


```
    "item": [
  {
    "linkId": "PR-name-text",
    "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.name.given",
    "text": "First Name",
    "_text": {
      "extension": [
        {
          "url": "http://hl7.org/fhir/StructureDefinition/translation",
          "extension": [
            {
              "url": "lang",
              "valueCode": "sw"
            },
            {
              "url": "content",
              "valueString": "Jina la kwanza"
            }
          ]
        }
      ]
    },
    "type": "string",
    "required": true
  }
]
```

Language switching can be accessed and toggle from the menu item in the Navigation drawer menu as depicted in Figure 8 and Figure 9



<img src="https://user-images.githubusercontent.com/4540684/164892677-62a34ed3-649e-4919-833a-ab03e422fe0a.png" width="200" height="400" />

Figure 8. Navigation Drawer 



<img src="https://user-images.githubusercontent.com/4540684/164892685-ea50931e-a88d-442a-af5a-122ecfa3f9f5.png" width="200" height="400" />

Figure 9. Switch User Language


#Chapter 4 - Immunization workflow


## 4.1 Navigating the Vaccination user journey

The side navigation drawer as depicted in Figure 10, gives you access to the following menu items and selections namely 



1. Number of Clients enrolled 
2. Multi-Language select switcher option
3. Log out of the application  


<img src="https://user-images.githubusercontent.com/4540684/164892696-8e0ea135-4b71-46b7-bbf2-2fa5250bd40c.png" width="200" height="400" />


Figure 10. Navigation Drawer


## 4.2 Home, Landing page, Enrolled clients 

On successful sync of all patient data, all the enrolled patients can be accessed via a paginated Patient list with an option to search and view the different immunization statuses of the clients at a glance. The Landing page also gives you access to the :



1. Access to the Navigation drawer button 
2. Search
3. Show overdue toggle 
4. Pagination icons 
5. Register new clients button 
6. Patient List with a status 



<img src="https://user-images.githubusercontent.com/4540684/164892706-2aea1282-86a2-45ba-aa7a-2a8ee34c878e.png" width="200" height="400" />


Figure 11. Patient Listing 


## 4.3 User profile   

A user can be able to access a patient's profile by clicking on the patient from the patient list. The profile page allows the user to view details for the user



1. Patient Family and Given name 
2. Gender 
3. Age - Years
4. Vaccine received 
5. Status
6. Vaccine received 
8. Provider  - Facility clinic   



<img src="https://user-images.githubusercontent.com/4540684/164892929-f88f6cdf-0a4d-4e51-bec0-c7ed09acddcf.png" width="200" height="400" />


Figure 12. User Profile


## 4.4	Register  

The application allows you to register a new client-patient by enrolling them to the platform and also collect information such as 



1. Name 
2. Gender 
3. Date of Birth 
4. Is it Active? 



<img src="https://user-images.githubusercontent.com/4540684/164893422-1425c167-447a-4111-844f-15c1e1a17e85.png" width="200" height="400" />

Figure 13. Registration page 

<img src="https://user-images.githubusercontent.com/4540684/164893480-2667a249-63d2-4029-8f4d-0c1a8a87f43b.png" width="200" height="400" />

Figure 14. Populated Registration page

In-app validation is currently available to validate all mandatory and optional fields. Figure 12. given as an impression of how mandatory fields will be highlighted while Figure 13 gives an overview of a correlate filled in registration survey. Once you have successfully submitted a new client you are taken back to the Patient List view on the home landing page as seen in Figure 14. 


<img src="https://user-images.githubusercontent.com/4540684/164893512-ac5b03fd-333a-4acb-824f-7edf89dc925a.png" width="200" height="400" />


Figure 15.. Landing Page post-registration


## 4.5 Search 


### 4.5.1 Search by  HCID  - Scan Barcode   

The current app implementation uses the Google ML Kit barcode scanner.  Reads most standard formats



1. Linear formats: Codabar, Code 39, Code 93, Code 128, EAN-8, EAN-13, ITF, UPC-A, UPC-E
2. 2D formats: Aztec, Data Matrix, PDF417, QR Code

This has also the following feature 



1. Automatic format detection - Scan for all supported barcode formats at once without having to specify the format you're looking for, or boost scanning speed by restricting the detector to only the formats you're interested in.
2. Extracts structured data - Structured data that's stored using one of the supported 2D formats are automatically parsed. Supported information types include URLs, contact information, calendar events, email addresses, phone numbers, SMS message prompts, ISBNs, WiFi connection information, geographic location, and AAMVA-standard driver information.
3. Works with any orientation - Barcodes are recognized and scanned regardless of their orientation: right-side-up, upside-down, or sideways.
4. Runs on the device - Barcode scanning is performed completely on the device, and doesn't require a network connection.

_Note that this API does not recognize barcodes in these forms:_



* _1D Barcodes with only one character_
* _Barcodes in ITF format with fewer than six characters_
* _Barcodes encoded with FNC2, FNC3 or FNC4_
* _QR codes generated in the ECI mode_

In the current covax demo app we have the following use case implemented 



1. A Patient  will present a paper-based card with an HCID  
2. The user of the app will scan the HCID to check whether the user is enrolled or not. See Figure 16 and Figure 17
3. If enrolled, the app opens the patient’s user profile as shown in Figure 18
4. If not the scan does not find a matching profile a registration page with the prepopulated HCID is open for the user to enrol the patient on the system. See figure 19 and Figure 20



<img src="https://user-images.githubusercontent.com/4540684/164893542-49aca214-a53c-4907-938b-914725da2455.png" width="200" height="400" />

Figure 16. Scan for a Pre Existing client HCID embedded as a Barcode 

 

<img src="https://user-images.githubusercontent.com/4540684/164893549-6be68ac1-6c1b-4dfa-9567-3dd5233b9ff5.png" width="200" height="400" />


Figure 17. Barcode retrieved 



<img src="https://user-images.githubusercontent.com/4540684/164893588-79aab932-bd56-4773-ba26-b8b98315e64e.png" width="200" height="400" />

Figure 18. Successful scan to profile page 



<img src="https://user-images.githubusercontent.com/4540684/164893615-9711bf4f-4084-4002-ba3a-8c7f71daa9ba.png" width="200" height="400" />

Figure 19. Scan for a non-registered HCID 


<img src="https://user-images.githubusercontent.com/4540684/164893641-02ae241a-c773-48a6-83d1-e9324c7e2a26.png" width="200" height="400" />

Figure 20. Pre-populated registration page with HCID

 

 

 

  


### 4.5.2 Text search       

The application also has the ability to allow the user to use text-based search that tries to filter enrolled patients on the app based on their:



1. Family name 
2. Given name 
3. HCID 

As you enter the text on key-press up the search filter will try to match the results and present them on a paginated results page as seen in Figure 21.


<img src="https://user-images.githubusercontent.com/4540684/164893692-4c0edecc-378a-4aef-bc5f-ab9b1025a425.png" width="200" height="400" />


Figure 21. Search by Patient 


## 4.6  Filter Overdue 

On the top bar navigation, we have a filter toggle that allows you to easily filter all the patients who are overdue i.e. Have received the 1st dose but have skipped or gone past the next due date to receive the next shot in a multi-dose vaccine as illustrated in Figure 22.



<img src="https://user-images.githubusercontent.com/4540684/164893723-a289e816-053a-4253-ab80-af5e91de0439.png" width="200" height="400" />

Figure 22. Filter Overdue patients 


## 4.7 	Immunization status  - Profiles  

The application has 3 main statuses, colour-coded using Amber and Green, for the patients and can be viewed in Figure 23, Figure 24 and Figure 25 mainly listed below :



1. No vaccine received  = Not Immune  - Amber
2. 1st Dose  = Not Immune- Amber
3. Overdue   = Not Immune- Amber
4. All dose received  = Immune  - Green



<img src="https://user-images.githubusercontent.com/4540684/164893757-3059d968-5ae4-4d15-942f-063c927db283.png" width="200" height="400" />

Figure 23. Patient profile with no Vaccine administered 

      
<img src="https://user-images.githubusercontent.com/4540684/164893734-6bffcbb2-c7f1-4c33-8862-c1cd5822e9a5.png" width="200" height="400" />

Figure 24. Patient profile with 1st Dose administered 



<img src="https://user-images.githubusercontent.com/4540684/164893738-1bbb7311-c005-4d1a-a1dd-123a6dde0e2f.png" width="200" height="400" />

Figure 25. Patient fully immunized 


## 4.8 Record Immunizations


### 4.8.1 	Add Vaccine 


#### 4.8.1.1 Vaccine codes

The following vaccine codes were used based on the WHO Family of International Classifications (WHO-FIC). For the latest release of ICD-11  go to ICD-11 Home Page via [https://icd.who.int/](https://icd.who.int/) 



1. Pfizer–BioNTech   [https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f667112200](https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f667112200) 
    1. Code  - XM8NQ0 Comirnaty®Pfizer BioNTech - Comirnaty
    2. Type - XM0GQ8 COVID-19 vaccine, RNA based
    3. System - [https://icd.who.int/dev11/l-m/en](https://icd.who.int/dev11/l-m/en) 
    4. Manufacturer - dependent on country  - BioNTech
2. Moderna  [https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f1211296175](https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f1211296175) 
    5. Code  - XM3DT5 COVID-19 Vaccine Moderna
    6. Type - XM0GQ8 COVID-19 vaccine, RNA based
    7. System - [https://icd.who.int/dev11/l-m/en](https://icd.who.int/dev11/l-m/en) 
    12. Manufacturer - dependent on country  - Moderna
3. AstraZeneca   [https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f473439328](https://icd.who.int/dev11/l-m/en#/http%3a%2f%2fid.who.int%2ficd%2fentity%2f473439328) 
    13. Code  - XM4YL8 COVID-19 Vaccine AstraZeneca
    17. Type - XM9QW8 COVID-19 vaccine, non-replicating viral vector
    18. System - [https://icd.who.int/dev11/l-m/en](https://icd.who.int/dev11/l-m/en) 
    19. Manufacturer - dependent on country  - Oxford
4. Janssen   - [https://icd.who.int/dev11/l-m/en#/http://id.who.int/icd/entity/1838464611](https://icd.who.int/dev11/l-m/en#/http://id.who.int/icd/entity/1838464611) 
    20. Code  - XM6QV1 COVID-19 Vaccine Janssen
    21. Type - XM9QW8 COVID-19 vaccine, non-replicating viral vector
    22. System - [https://icd.who.int/dev11/l-m/en](https://icd.who.int/dev11/l-m/en) 
    23. Manufacturer - dependent on country  - Johnson & Johnson 


#### 4.8.1.2 Practitioner & Roles

The application at the moment does not allow syncing of practitioner details from HAPI due to privacy concerns and lack of a clearly defined use case for Location hierarchy sync 


#### 4.8.1.3 Facility Location

The application at the moment does not allow syncing of Location details from HAPI due to privacy concerns and lack of a clearly defined use case for Location hierarchy sync 


#### 4.8.1.4 Immunization flow

The application currently supports Immunization against the COVID-19 virus based on the 4 vaccines provided in section 4.8.1.1. A user will be required to either enrol or search for a patient before recording the administering of a vaccine for a given patient. 

Once a user is retrieved from the patient list you can either record the 1st dose if not yet recorded, 2nd Dose for a Dual dose vaccine and then either record the receiving of a vaccine shot in the case of an overdue fulfilled vaccination. This is illustrated in Figure 27 

Recording can either be initiated from the Patient list depicted in Figure 26 or from the selected user profile depicted in Figure 28.     



<img src="https://user-images.githubusercontent.com/4540684/164893779-ad25e621-83f8-438d-b75a-6cd9856e8a3c.png" width="200" height="400" />
Figure 26. Search for a Patient or Register a new Patient 


<img src="https://user-images.githubusercontent.com/4540684/164893088-79148a7c-ab9d-4840-b298-c222cb09fb76.png" width="200" height="400" />

Figure 27. Available vaccine page 

  

<img src="https://user-images.githubusercontent.com/4540684/164893076-bcf83df1-b201-4801-a4b0-429a73634d40.png" width="200" height="400" />


Figure 28. 1st Dose administered



<img src="https://user-images.githubusercontent.com/4540684/164893075-47b5e585-98e7-49b8-af42-83caa4c613b4.png" width="200" height="400" />


Figure 29. Patient fully immunized 


## 4.9 FHIR Web Administration portal

Go to the server url for your project, e.g: [https://fhir-web.opensrp-stage.smartregister.org/](https://fhir-web.opensrp-stage.smartregister.org/) and then enter your username and password. Contact your project administrator for login details, if necessary. Click on Patients to view all patients in the FHIR Web portal 



<img width="1731" alt="2  home" src="https://user-images.githubusercontent.com/4540684/164893918-ef3e31b0-891f-437f-ba1b-d52e56b0f9a8.png">


Figure 30. FHIR Web Landing page 

From the Left Navigation bar, select the Patients Tab to view all the patients available in the portal as illustrated in Figure 31.  You can be able to search and retrieve a patient based on their :



1. Family Name
2. Given name 
3. Identifier (HCID)

 
<img width="1731" alt="3  Patient listing" src="https://user-images.githubusercontent.com/4540684/164893058-82929406-feb3-4648-acf5-980046434cee.png">


Figure 31. FHIR Web Patient listing 

The search filter is illustrated in Figure 32.  



<img width="1731" alt="4  Patient Search" src="https://user-images.githubusercontent.com/4540684/164893046-af12af9e-e57b-4d06-92b4-2a6cd6fe9b51.png">

Figure 32. FHIR Web Search for patient 


Figure 33 Illustrates the patient profile that highlights : 

1. Patient Demographics details  
2. Document Reference 
3. Immunization Recommendation 
4. Immunization 
5. Encounter
6. Practitioner
7. Location 
9. Organisation


<img width="1731" alt="5  Patient Profile" src="https://user-images.githubusercontent.com/4540684/164893040-11eb337a-0c26-4519-9c00-7d5a3e524af1.png">


Figure 33. FHIR Web Patient profile  - Patient details 

All Vaccination encounters can be accessed via the Encounters Tab on the patient’s profile as illustrated in Figure 34.  



<img width="1731" alt="6  Encounters" src="https://user-images.githubusercontent.com/4540684/164893034-43edcb8c-e80b-4977-9441-8691c9016619.png">
Figure 34. FHIR Web Patient profile  - Patient encounter  

All Immunizations recorded can be accessed via the Encounters Tab on the patient’s profile as illustrated in Figure 35.  


<img width="1731" alt="8  QR codes" src="https://user-images.githubusercontent.com/4540684/164892985-38094925-f38e-451f-af4e-d1b12ddd19c7.png">
Figure 35. FHIR Web Patient profile  - Patient immunization profile  

A user can also view the Generated Smart QR codes with Data elements for the DDCC: VC.

**_NB. This is a Proof of Concept that is not yet completed and was done for demonstration purposes only. _**


<img width="1731" alt="9  Download QR" src="https://user-images.githubusercontent.com/4540684/164893002-92df7151-419f-4f5c-95f5-f4c26a339d4f.png">
Figure 36. FHIR Web Patient profile  - Patient SVC attachments  
