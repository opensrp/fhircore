"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[163],{3905:(e,t,n)=>{n.d(t,{Zo:()=>d,kt:()=>h});var a=n(7294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function l(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},o=Object.keys(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var p=a.createContext({}),s=function(e){var t=a.useContext(p),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},d=function(e){var t=s(e.components);return a.createElement(p.Provider,{value:t},e.children)},c={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},u=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,o=e.originalType,p=e.parentName,d=l(e,["components","mdxType","originalType","parentName"]),u=s(n),h=r,m=u["".concat(p,".").concat(h)]||u[h]||c[h]||o;return n?a.createElement(m,i(i({ref:t},d),{},{components:n})):a.createElement(m,i({ref:t},d))}));function h(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=n.length,i=new Array(o);i[0]=u;var l={};for(var p in t)hasOwnProperty.call(t,p)&&(l[p]=t[p]);l.originalType=e,l.mdxType="string"==typeof e?e:r,i[1]=l;for(var s=2;s<o;s++)i[s]=n[s];return a.createElement.apply(null,i)}return a.createElement.apply(null,n)}u.displayName="MDXCreateElement"},3499:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>p,contentTitle:()=>i,default:()=>c,frontMatter:()=>o,metadata:()=>l,toc:()=>s});var a=n(7462),r=(n(7294),n(3905));const o={sidebar_position:2},i="Getting started for platform developers",l={unversionedId:"engineering/android-app/getting-started/readme",id:"engineering/android-app/getting-started/readme",title:"Getting started for platform developers",description:"Android CI with Gradle",source:"@site/docs/engineering/android-app/getting-started/readme.mdx",sourceDirName:"engineering/android-app/getting-started",slug:"/engineering/android-app/getting-started/",permalink:"/engineering/android-app/getting-started/",draft:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/docs/engineering/android-app/getting-started/readme.mdx",tags:[],version:"current",sidebarPosition:2,frontMatter:{sidebar_position:2},sidebar:"defaultSidebar",previous:{title:"Introduction",permalink:"/engineering/android-app/introduction/"},next:{title:"Platform Components",permalink:"/engineering/android-app/platform-components/"}},p={},s=[{value:"Build setup",id:"build-setup",level:3},{value:"Gateway vs Non Gateway backend",id:"gateway-vs-non-gateway-backend",level:4},{value:"App release",id:"app-release",level:3},{value:"Application architecture",id:"application-architecture",level:3},{value:"Project Structure",id:"project-structure",level:3},{value:"Package structure",id:"package-structure",level:3},{value:"<code>data</code>",id:"data",level:4},{value:"<code>ui</code>",id:"ui",level:4},{value:"<code>util</code>",id:"util",level:4},{value:"Resources",id:"resources",level:3},{value:"Running the documentation locally",id:"running-the-documentation-locally",level:3}],d={toc:s};function c(e){let{components:t,...n}=e;return(0,r.kt)("wrapper",(0,a.Z)({},d,n,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("h1",{id:"getting-started-for-platform-developers"},"Getting started for platform developers"),(0,r.kt)("p",null,(0,r.kt)("a",{parentName:"p",href:"https://github.com/opensrp/fhircore/actions/workflows/ci.yml?query=branch%3Amain"},(0,r.kt)("img",{parentName:"a",src:"https://github.com/opensrp/fhircore/actions/workflows/ci.yml/badge.svg?branch=main",alt:"Android CI with Gradle"})),"\n",(0,r.kt)("a",{parentName:"p",href:"https://codecov.io/gh/opensrp/fhircore"},(0,r.kt)("img",{parentName:"a",src:"https://codecov.io/gh/opensrp/fhircore/branch/main/graph/badge.svg?token=IJUTHZUGGH",alt:"codecov"})),"\n",(0,r.kt)("a",{parentName:"p",href:"https://opensource.org/licenses/Apache-2.0"},(0,r.kt)("img",{parentName:"a",src:"https://img.shields.io/badge/License-Apache_2.0-blue.svg",alt:"License"})),"\n",(0,r.kt)("a",{parentName:"p",href:"https://chat.fhir.org/#narrow/stream/370552-OpenSRP"},(0,r.kt)("img",{parentName:"a",src:"https://img.shields.io/badge/zulip-join_chat-brightgreen.svg",alt:"project chat"}))),(0,r.kt)("h3",{id:"build-setup"},"Build setup"),(0,r.kt)("p",null,"Begin by cloning this repository. Ensure you have JAVA 11 installed, and setup Android studio to use the Java 11 JDK for this project."),(0,r.kt)("p",null,"Update ",(0,r.kt)("inlineCode",{parentName:"p"},"local.properties")," file by providing the required Keycloak credentials to enable syncing of data to and from the HAPI FHIR server:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre"},'OAUTH_BASE_URL=https://keycloak-stage.smartregister.org/auth/realms/FHIR_Android/\nOAUTH_CIENT_ID="provide client id"\nOAUTH_CLIENT_SECRET="provide client secret"\nOAUTH_SCOPE=openid\nFHIR_BASE_URL=https://fhir.labs.smartregister.org/fhir/\n')),(0,r.kt)("h4",{id:"gateway-vs-non-gateway-backend"},"Gateway vs Non Gateway backend"),(0,r.kt)("p",null,"Currently the codebase supports building a release that uses the ",(0,r.kt)("a",{parentName:"p",href:"https://github.com/opensrp/fhir-gateway"},"Proxy/Gateway")," and one that doesn't. Proxy is default but if you backend server is not using the ",(0,r.kt)("a",{parentName:"p",href:"https://github.com/opensrp/fhir-gateway/releases"},"FHIR Gateway")," you need to specify this in the build configuration. For now, inorder to switch between the two one needs to update ",(0,r.kt)("a",{parentName:"p",href:"https://github.com/opensrp/fhircore/blob/main/android/engine/build.gradle.kts#L30"},"this file here")," and set the value to ",(0,r.kt)("inlineCode",{parentName:"p"},"true")," "),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-kotlin"},'buildConfigField(\n      "boolean",\n      "IS_NON_PROXY_APK",\n      "true",\n    )\n')),(0,r.kt)("h3",{id:"app-release"},"App release"),(0,r.kt)("p",null,"In order for the ",(0,r.kt)("inlineCode",{parentName:"p"},"assembleRelease")," and/or ",(0,r.kt)("inlineCode",{parentName:"p"},"bundleRelease")," Gradle tasks to work for instance when you need to generate a signed release version of the APK (or AAB), a keystore is required."),(0,r.kt)("p",null,"Generate your own release keystore using the ",(0,r.kt)("inlineCode",{parentName:"p"},"keytool")," utility (installed as part of the java runtime) by running the following command:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-sh"},"keytool -genkey -v -keystore fhircore.keystore.jks -alias <your_alias_name> -keyalg RSA -keysize 4096 -validity 1000\n")),(0,r.kt)("p",null,"Place the Keystore file in your user(home) directory i.e. ",(0,r.kt)("inlineCode",{parentName:"p"},"/Users/username/fhircore.keystore.jks")," for Windows or ",(0,r.kt)("inlineCode",{parentName:"p"},"~/fhircore.keystore.jks")," for Unix based systems."),(0,r.kt)("p",null,"Next, create the following SYSTEM_VARIABLEs and set their values accordingly: ",(0,r.kt)("inlineCode",{parentName:"p"},"KEYSTORE_ALIAS"),", ",(0,r.kt)("inlineCode",{parentName:"p"},"KEYSTORE_PASSWORD"),", ",(0,r.kt)("inlineCode",{parentName:"p"},"KEY_PASSWORD")," "),(0,r.kt)("p",null,(0,r.kt)("strong",{parentName:"p"},"Note:")," Assign the generated keystore values to the SYSTEM_VARIABLEs listed above. Also note, if your platform doesn't prompt you for a second password when generating the Keystore (e.g. of type PKCS12) then both the KEYSTORE_PASSWORD and KEY_PASSWORD should have the same value."),(0,r.kt)("p",null,"You can alternatively store the above credentials in a file named ",(0,r.kt)("inlineCode",{parentName:"p"},"keystore.properties"),". Just be careful to include this file in ",(0,r.kt)("inlineCode",{parentName:"p"},".gitignore")," to prevent leaking secrets via git VCS."),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre"},"KEYSTORE_PASSWORD=xxxxxx\nKEYSTORE_ALIAS=xxxxxx\nKEY_PASSWORD=xxxxxx\n")),(0,r.kt)("p",null,"Lastly, you can also provide the ",(0,r.kt)("inlineCode",{parentName:"p"},"local.properties")," and/or the ",(0,r.kt)("inlineCode",{parentName:"p"},"keystore.properties")," file as a gradle property using via the property names ",(0,r.kt)("inlineCode",{parentName:"p"},"localPropertiesFile")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"keystorePropertiesFile")," respectively. The value of the properties file should be its absolute file path. "),(0,r.kt)("p",null,"For example, to build the eCHIS release from the terminal you can run the command below:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-sh"},"./gradlew assembleEchisRelease --stacktrace -PlocalPropertiesFile=/Abolute/Path/To/echis.local.properties -PkeystorePropertiesFile=/Absolute/Path/To/echis.keystore.properties \n")),(0,r.kt)("p",null,(0,r.kt)("strong",{parentName:"p"},"Note:")," The app supports a variant mode that does not require the FHIR Gateway/Proxy to be integrated. To build the same APK as a NON-PROXY variant set the ",(0,r.kt)("inlineCode",{parentName:"p"},"isNonProxy")," gradle property to ",(0,r.kt)("inlineCode",{parentName:"p"},"true")," e.g."),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-sh"},"./gradlew assembleEchisRelease --stacktrace -PisNonProxy=true -PlocalPropertiesFile=/Abolute/Path/To/proxy.echis.local.properties -PkeystorePropertiesFile=/Absolute/Path/To/echis.keystore.properties \n")),(0,r.kt)("p",null,"Refer to the following links for more details:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html"},"Java Key and Certificate Management Tool")),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://developer.android.com/studio/publish/app-signing"},"Signing Android apps"))),(0,r.kt)("h3",{id:"application-architecture"},"Application architecture"),(0,r.kt)("p",null,"FHIR Core is based on MVVM Android application architecture. It also follows the recommended ",(0,r.kt)("a",{parentName:"p",href:"https://developer.android.com/jetpack/guide"},"Repository Pattern")," on its data layer. The diagram below shows the different layers of the application structure and how they interact with eachother."),(0,r.kt)("p",null,"At the core is Android FHIR SDK which provides Data Access API, Search API, Sync API, Smart Guidelines API and Data Capture API."),(0,r.kt)("h3",{id:"project-structure"},"Project Structure"),(0,r.kt)("p",null,"The project currently consists an application module (",(0,r.kt)("inlineCode",{parentName:"p"},"quest"),") and two Android library modules (",(0,r.kt)("inlineCode",{parentName:"p"},"engine")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"geowidget"),"). The ",(0,r.kt)("inlineCode",{parentName:"p"},"geowidget")," module contains implementation for intergrating Map views to FHIR Core. ",(0,r.kt)("inlineCode",{parentName:"p"},"engine")," module contains shared code."),(0,r.kt)("h3",{id:"package-structure"},"Package structure"),(0,r.kt)("p",null,(0,r.kt)("inlineCode",{parentName:"p"},"quest")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"geowidget")," modules packages are grouped based on features. ",(0,r.kt)("inlineCode",{parentName:"p"},"engine")," module on the other hand uses a hybrid approach, combining both layered and feature based package structure."),(0,r.kt)("p",null,"At a higher level every module is at least organized into three main packages, namely:"),(0,r.kt)("h4",{id:"data"},(0,r.kt)("inlineCode",{parentName:"h4"},"data")),(0,r.kt)("p",null,"This package is used to holds classes/objects implementations for accessing data view the Android FHIR SDK APIs. The ",(0,r.kt)("inlineCode",{parentName:"p"},"data")," package for ",(0,r.kt)("inlineCode",{parentName:"p"},"engine")," module is further sub-divided into two sub-packages that is ",(0,r.kt)("inlineCode",{parentName:"p"},"local")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"remote"),". ",(0,r.kt)("inlineCode",{parentName:"p"},"local")," directory holds the implementation for accessing the Sqlite database whereas",(0,r.kt)("inlineCode",{parentName:"p"},"remote")," directory contains implementation for making http requests to HAPI FHIR server backend."),(0,r.kt)("h4",{id:"ui"},(0,r.kt)("inlineCode",{parentName:"h4"},"ui")),(0,r.kt)("p",null,"This package mostly contains Android ",(0,r.kt)("inlineCode",{parentName:"p"},"Activity"),", ",(0,r.kt)("inlineCode",{parentName:"p"},"Fragment"),", ",(0,r.kt)("inlineCode",{parentName:"p"},"ViewModel"),", and ",(0,r.kt)("inlineCode",{parentName:"p"},"Composable")," functions for rendering UI."),(0,r.kt)("h4",{id:"util"},(0,r.kt)("inlineCode",{parentName:"h4"},"util")),(0,r.kt)("p",null,"This package is used to hold any code that shared internally typically implemented as Kotlin extensions. Other utilities use kotlin ",(0,r.kt)("inlineCode",{parentName:"p"},"object")," to implement singletons."),(0,r.kt)("p",null,"Conventionally, classes are further organized into more cohesive directories within the main packages mentioned above. This should allow minimal updates when code is refactored by moving directories."),(0,r.kt)("h3",{id:"resources"},"Resources"),(0,r.kt)("p",null,"Refer to the following links for more details:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://developer.android.com/jetpack/guide"},"Android App Architecture Guide")," - Learn more about Android App Architecture"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://developer.android.com/jetpack/compose"},"Jetpack Compose")," - Learn more about Jetpack Compose")),(0,r.kt)("h3",{id:"running-the-documentation-locally"},"Running the documentation locally"),(0,r.kt)("p",null,"To run the documentation locally navigate to the repository root directory and run:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre"},"npm install\nnpx docusaurus start\n")))}c.isMDXComponent=!0}}]);