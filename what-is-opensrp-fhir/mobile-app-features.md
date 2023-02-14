---
description: >-
  Features of the OpenSRP Android app, used by healthcare workers to record
  patient visits.
---

# Mobile app features

## Record services

Patient and health service information is captured using forms with questions and fields. The app keeps the interaction as simple as possible and has error-checking to keep entered data accurate.

A range of fields are available: boolean, single choice, multiple choice, dropdown, text, date picker, date and time picker, slider, attachment. In addition, forms include functionality for showing modals, showing images in questions, showing images next to multiple choice options, having repeated groups, autocomplete, and help.

Forms can be edited on the same day they were entered. Editing is disabled on the following day.

Data cannot be pulled from previous forms and entered into an active form. However, data from the same form can be used later in the form.

Form styling is flexible and applies to all forms in an OpenSRP app instance. It is possible to style elements such as the question label text, radio buttons, drop down layout, and error text, submit button, and many other visual items.

## Patient and family registration&#x20;

A common step in community health program is creating a list of all persons and households in a health worker's area. OpenSRP supports registering households and people quickly using simple forms.

Patients and households are associated to a health worker through a location. A health worker is assigned to a specific area or set of areas, and every person they register is assigned to the same area or set of areas.

In cases where patients visit or health workers cover multiple clinics across area, both the patient and the health worker would be assigned to a higher level location. For example, instead of being assigned to a village, they would be assigned to a facility area or county. That would result in a patient showing up on any of the facility patient lists in that higher level area, and for that patients medical record to be synced to the health worker's device.

## Finding patients

Navigating to a patient quickly is paramount for health work, especially when in the community. Because it is common for OpenSRP to be used in places where many people may share similar names, we offer many ways for patient look-up.&#x20;

Patients can be searched by `NAME` or `ID`. This is a manual search where the patient list is updated after two characters are entered (the results update without pressing "enter"). The search will display patients only assigned to the health worker ([Read: how patients are assigned to health workers](mobile-app-features.md#patient-and-family-registration))

The `ID` can be a national ID number, a local ID number, or an app-generated ID. For a national ID number or local ID number, there is an option to either have the IDs be non-unique (there is no restriction on reusing IDs) or system-unique (it is enforced that IDs are not shared across the system). App-generated IDs are always unique.

Patients are listed in order of time overdue in each patient register. For households and patients, that means households with more tasks and more overdue tasks are listed first, and households with fewer tasks and fewer overdue tasks are listed last.

## Care plans

Care plans are the health service tasks and protocols a patient should receive depending on their status or condition, with the purpose of making sure the right services are provided to patients when they are supposed to be given. The tasks in the care plan are scheduled at the patient receives the updated status.&#x20;

For example, a patient who has just been recorded as pregnant will receive a schedule of antenatal visit tasks associated with their gestational age. Each task is completed based on completing a form associated with that task. For example, an antenatal visit in pregnancy week 32 might include a form to check for pregnant danger signs, baby heart beats per minute, and a counseling session, which the health worker can fill out.

Tasks can be constrained with dates that make them inactive, active/due, overdue, and expired. Taking the example pregnancy scenario above, the week 32 pregnancy visit task can be inactive until week 30, then become active for weeks 30–32, then overdue in weeks 32-35, and expire after week 35.

OpenSRP FHIR has prebuilt care plans for antenatal care, postnatal care, childhood health, and maternal health.&#x20;

## Offline mode

OpenSRP stores patient records entirely offline and is able to register patients and record services without internet or data access.

#### Data syncing

When a user is offline and then gets access to the internet with the app is open, records sync automatically with the centralized cloud-based server. Syncing can take place at specific intervals (in order to save battery life) or triggered manually by the user.

When offline, the most up-to-date patient data exists on the user's device. If a patient record is recorded on another offline device, the data from the more recent encounter is used.

#### Fully offline record sync

Sometimes, community health workers or other devices can be so remote as to never be connected to the internet or data connection. In this scenario, OpenSRP supports device-to-device data transfer to update a centralized cloud-based server. The way it works is a person with a device who does access the internet meets with the person with a device who never connects to the internet. Patient records are transferred up the line, from device to device, until records can be synced to the cloud to an internet-connected device.

Data can be transferred without exposing the patient records in the OpenSRP app. For example, if a health worker does not have access to another health worker's patient data because they are assigned to a different location, data can still be transferred through their device and to the cloud.

## Tasks

Tasks are used to identify patients that are due for health services. They are meant to help health workers prioritize who to visit at any given time by prominently appearing in the register list views.

Tasks in OpenSRP can fit into five categories: (1) inactive and not completable, (2) inactive but completable, (3) active and due, (4) overdue, and (5) expired. Tasks are represented in the app in both register lists and profile as \_\_\_\_\_ colors. They are listed in order of due date.

Tasks can be due on a specific day or for a time period. The primary settings for a task are the health service form that closes the task and time periods for when the task becomes active, remains active, becomes overdue, and expires.

## Reports

[https://docs.google.com/document/d/18FVsOeym3iT0qhnsAXKlfq9RPVQsV8x0jDZgtlUBwdQ/edit#heading=h.raxzz8kpbpmr](https://docs.google.com/document/d/18FVsOeym3iT0qhnsAXKlfq9RPVQsV8x0jDZgtlUBwdQ/edit#heading=h.raxzz8kpbpmr), [https://docs.communityhealthtoolkit.org/apps/features/reports/](https://docs.communityhealthtoolkit.org/apps/features/reports/), [https://docs.simple.org/readme/simple-app-features#progress-reports](https://docs.simple.org/readme/simple-app-features#progress-reports)

## Stock control or commodity management

[https://docs.simple.org/readme/simple-app-features#drug-stock-reporting](https://docs.simple.org/readme/simple-app-features#drug-stock-reporting), [https://docs.google.com/document/d/18FVsOeym3iT0qhnsAXKlfq9RPVQsV8x0jDZgtlUBwdQ/edit#heading=h.nct1tawcwukq](https://docs.google.com/document/d/18FVsOeym3iT0qhnsAXKlfq9RPVQsV8x0jDZgtlUBwdQ/edit#heading=h.nct1tawcwukq),&#x20;

VHTs are required to provide treatments to U5s who are unwell due to common childhood illnesses like malaria, diarrhea and pneumonia. In cases where the U5s do not get better or show danger signs, they are referred to the health facility and followed up by the VHT thereafter, to ensure that they get better. The VHTs are thus normally restocked with drugs/commodities following the National Medical Stores schedule that operates on a two-month or bi-monthly cycle. The drugs are usually dispatched to health facilities before they are passed on to the VHTs. VHTs use a consumption log to document the drugs they receive and dispense in the community and are required to present a key accountability document called HMIS VHT 004 Summary Form for Consumption and Requisition  to their respective Health Assistant (at the facility where they are attached) in order to receive drugs and commodities (aka refill). VHTs are then given drugs and commodities based on any balances they may have.

System Workflows

Recording new stock

![](https://lh5.googleusercontent.com/iFlwoLZXM2LpCi\_RgPmzhvcPc3HR3ZMV23UKYfeRVblRPZVEJET\_gAvOksWBa8N9SJzAR-Wae\_5e9sIO3XvkWMw9G0cXhgpSPm6LTsW9HRkrKxM1k-TjscBFwFFMe34Rm0USbauk2gJLNnGW7VqYeW3aixXBlN9UwpfSDVtHGMfPd\_8wFiai4arwOY9eOA)

\


Recording of commodity consumption during service delivery

![](https://lh4.googleusercontent.com/oSIY7bnRL9d49Upg6rEUBOBacGtQ5ChszqaVkBqXv9B9IQcTbCc02y6VS25RamSI1bAGAG3IJ3edfm4rtAG2JkCP929WQSPNThRl8YZXd4GZsNy6MM8CBqi9rt2aL7Hd435Pw8cE7xrsgbbeIX1Xi6X\_GNzTXSZYd5TaAtEu2msJN7nRcVbLADR14167Lg)

The following commodities will be consumed during service delivery

| <p>Visit Type</p><p>(Questionnaire type)</p> | Data Dictionary                                                            | <p>Commodity<br>to be (potentially) issued </p>                                                                                                                                     |
| -------------------------------------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Family planning visit                        | <ul><li>Family Planning enrollment</li><li>Family Planning visit</li></ul> | <ol><li>Male Condoms</li><li>Female Condom</li><li>Combined oral pill</li></ol>                                                                                                     |
| Sick Child Visit                             | <ul><li>Sick child treatment and referral</li></ul>                        | <ol><li>Artesunate 100mg Suppository</li><li>ACT</li><li>Amoxicillin 250mg Dispersible Tablet</li><li>ORS Sachet</li><li>Zinc Sulfate</li><li>Rapid Diagnostic Test (RDT)</li></ol> |

\


Design Mocks&#x20;

[Link to the interactive prototype](https://www.figma.com/proto/9BV8bl86crmntmHcT4xR54/Uganda-iCOHS?node-id=15057%3A23573\&scaling=min-zoom\&page-id=14757%3A17082\&starting-point-node-id=15057%3A23573)

![](https://lh4.googleusercontent.com/AbBgpn93furrH3\_pkbsWuC\_do4Y0Z2o3e-LJlks9JaRxXX7UXuhU5ant84hfUYQ-eDHDPf03xLuKEUPWTYJ0-oO6cAufZRMkaXh3sSwoFDWsHqeW2aZRQr5GY9vSADaojRxI6JmsNcCZ8FEgGR\_I-4AUqcwnszE6dEcUhZyCxxfA2d\_IPeON-3tpSWTfVQ)![](https://lh6.googleusercontent.com/tGjGROn9P1Q\_3vAPBNwiokOte3\_cINEmSgvz8ZzJ502S3Ku30T22K5-skCcZeObN1dfx-qaUffKu\_Fo4d8aCQiiixspwNjAGchFfaLJId8r8EQ-nEAmFe19aVzQ8R0GcLR6cXjzaRVpmFT1N2l9HC2lCtIlBuzKEyfFPiw92NrIJtdfip3Gx\_3mYbkX0kA)![](https://lh4.googleusercontent.com/t2RGfSXtKATfjy\_jBM4\_2y9WWf0337sIGI80rPgtb0WMNibrBa8vY2NoBLzMWOLB4FhHxu2Ymq8CdkWm2PHZXLLp3Wh1pHklWil54tgyj6BK4VsMYOblKFx\_xprdXfXEvZeGcadh5Orkg6mbbYukfEUmfbNGKbZvmrHpC10fnia7rtO5m8A3NVZ\_SJ6FBg)

\


Future: Patient QR code, Language support, multi user syncing, child health growth monitoring, archived patients, uneditable fields vs. forms, risk or availability statuses, register filters, in app reports, referrals Referrals (referral form/field itself that creates the referral, referral follow up task with time, how to close. https://docs.google.com/document/d/18FVsOeym3iT0qhnsAXKlfq9RPVQsV8x0jDZgtlUBwdQ/edit#heading=h.yijz9n12jtt), supervisor app
