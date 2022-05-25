
**Matter for documentation **

**Repository** 

FHIR Core codebase  -  [https://github.com/opensrp/fhircore](https://github.com/opensrp/fhircore) 

**Programming Language ** - Kotlin 

**Code**



1. Hilt - [https://developer.android.com/training/dependency-injection/hilt-multi-module](https://developer.android.com/training/dependency-injection/hilt-multi-module)  
2. Build time vs Run time 
3. Dependencies Injection 

**UI**



1. Jetpack compose for navigation API  - [https://developer.android.com/jetpack/compose/navigation](https://developer.android.com/jetpack/compose/navigation) 

**Architectue** 



1. MVVM pattern 
2. Repository pattern to feedb view models with data
3. Android Single-Activity Architecture with Navigation Component - [https://oozou.com/blog/reasons-to-use-android-single-activity-architecture-with-navigation-component-36](https://oozou.com/blog/reasons-to-use-android-single-activity-architecture-with-navigation-component-36) 
4. Using composable fragment with navigation API 
5. Screen view model with components
6. View model business logic
7. Container to has different components e.g Screen component 
8. Component to hold sharedble  UI pieces  
9. Use navgraph to link different pagers  - [https://developer.android.com/reference/androidx/navigation/NavGraph#:~:text=NavGraph%20is%20a%20collection%20of,added%20to%20the%20back%20stack](https://developer.android.com/reference/androidx/navigation/NavGraph#:~:text=NavGraph%20is%20a%20collection%20of,added%20to%20the%20back%20stack). 

**Data layer / Database** 



1. Access to DB is via FHIR Engine from Android FHIR SDK 
2. We use Room  - [https://developer.android.com/jetpack/androidx/releases/room?gclid=CjwKCAjwp7eUBhBeEiwAZbHwkdmGs4cqYoOSGYdwG_HGxZn63-xcYgSPnwdyP6zzpznRHAwV9rKwaxoCiOEQAvD_BwE&gclsrc=aw.ds](https://developer.android.com/jetpack/androidx/releases/room?gclid=CjwKCAjwp7eUBhBeEiwAZbHwkdmGs4cqYoOSGYdwG_HGxZn63-xcYgSPnwdyP6zzpznRHAwV9rKwaxoCiOEQAvD_BwE&gclsrc=aw.ds) 
3. Database is encrypted  by default but can be un-encrypted in debug mode 
4. Alternatives
    1. Realm  -  https://realm.io/  - Realm is a fast, scalable alternative to SQLite with mobile to cloud data sync that makes building real-time, reactive mobile apps easy. 
    2. SQLite DB 
5. Domain 
    3. Using The DAO Pattern in Java /Kotlin 
        1. Register DAO factory 
            1. Family  
            2. ANC 
            3. HIV 
    4. Register repository 
        2. Load Register
        3. Count Register 
        4. Load Patient 
    5. App fearture manager - filter feature 
    6. Side menu factory 
    7. Paging source for patient pagination sent to view model 
    8. 

**Configs** 



1. App configs  - Composition resource 

**QA**



1. Unit tests 
2. Intergration tests
3. [https://app.codecov.io/gh/opensrp/fhircore](https://app.codecov.io/gh/opensrp/fhircore) 
