"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[2909],{760:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>a,contentTitle:()=>r,default:()=>h,frontMatter:()=>t,metadata:()=>l,toc:()=>c});var s=i(4848),o=i(8453);const t={},r="App Content Versioning",l={id:"engineering/app/system-design/configs-versioning",title:"App Content Versioning",description:"|||",source:"@site/docs/engineering/app/system-design/configs-versioning.md",sourceDirName:"engineering/app/system-design",slug:"/engineering/app/system-design/configs-versioning",permalink:"/engineering/app/system-design/configs-versioning",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/app/system-design/configs-versioning.md",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Tagging Resources",permalink:"/engineering/app/datastore/tagging"},next:{title:"Admin Dashboard",permalink:"/engineering/admin-dashboard/"}},a={},c=[{value:"Background",id:"background",level:2},{value:"Switch from Composition to ImplementationGuide",id:"switch-from-composition-to-implementationguide",level:2},{value:"Sequenced Work Plan",id:"sequenced-work-plan",level:2},{value:"Questions",id:"questions",level:3}];function d(e){const n={a:"a",code:"code",em:"em",h1:"h1",h2:"h2",h3:"h3",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",ul:"ul",...(0,o.R)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsx)(n.h1,{id:"app-content-versioning",children:"App Content Versioning"}),"\n",(0,s.jsxs)(n.table,{children:[(0,s.jsx)(n.thead,{children:(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.th,{}),(0,s.jsx)(n.th,{})]})}),(0,s.jsxs)(n.tbody,{children:[(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Date Submitted"}),(0,s.jsx)(n.td,{children:"March 25, 2024"})]}),(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Date Approved"}),(0,s.jsx)(n.td,{children:"April 19, 2024"})]}),(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Status"}),(0,s.jsx)(n.td,{children:"Complete"})]})]})]}),"\n",(0,s.jsx)(n.h2,{id:"background",children:"Background"}),"\n",(0,s.jsx)(n.p,{children:"We need to ensure compatibility between FHIR configs downloaded from the server and the version of the OpenSRP 2 app. With OpenSRP still in active development and ongoing changes to how configs are defined, implementing version-based content limitations is crucial to ensuring that the application functions correctly. This allows us to streamline the user experience and maintain consistency across different versions of the application."}),"\n",(0,s.jsx)(n.h2,{id:"switch-from-composition-to-implementationguide",children:"Switch from Composition to ImplementationGuide"}),"\n",(0,s.jsx)(n.p,{children:"OpenSRP currently uses a Composition resource to group the resources that define an OpenSRP 2 application. Storing versioning information in the Composition resource is non-trivial and ad hoc."}),"\n",(0,s.jsx)(n.p,{children:"An ImplementationGuide (IG) is designed with versioning support and rich metadata such as licensing information, authors, publication status, etc. IGs are the typical wrapper for a set of resources that define a healthcare workflow or system."}),"\n",(0,s.jsxs)(n.p,{children:["An IG has a ",(0,s.jsx)(n.code,{children:"useContext"})," field whose data type is a ",(0,s.jsx)(n.code,{children:"UsageContext"})," that has ",(0,s.jsx)(n.code,{children:"valueRange"})," (Range) as one of the allowed types. Range has ",(0,s.jsx)(n.code,{children:"low"})," and ",(0,s.jsx)(n.code,{children:"high"})," values which we will use define the app versions supported by the configs. It also has a ",(0,s.jsx)(n.code,{children:"version"})," field, we will use this to define the version of the content."]}),"\n",(0,s.jsxs)(n.p,{children:["The ",(0,s.jsx)(n.code,{children:"useContext.valueRange"})," defines the lowest and highest APK versions it is compatible with."]}),"\n",(0,s.jsxs)(n.p,{children:["The IG\u2019s ",(0,s.jsx)(n.code,{children:"definition"})," field maps to the ",(0,s.jsx)(n.code,{children:"section"})," field of the composition."]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"resource.reference"})," maps to ",(0,s.jsx)(n.code,{children:"section.focus.reference"})]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"resource.name"})," maps to ",(0,s.jsx)(n.code,{children:"section.focus.identifier.value"})]}),"\n"]}),"\n",(0,s.jsx)(n.p,{children:"ImplementationGuide"}),"\n",(0,s.jsx)(n.pre,{children:(0,s.jsx)(n.code,{children:"  - version\n  - useContext (valueRange)\n    - low\n    - high\n  - definition\n    - resource\n      - reference\n      - name\n"})}),"\n",(0,s.jsx)(n.p,{children:"ImplementationGuides are used to package all related resources to manage workflows e.g. immunization IG, malaria IG, HIV IG, etc. To align with how others use IGs, the ideal approach in OpenSRP would be to link all resources referenced in the Composition resource\u2019s section in the implementation guide and fully switch to using an IG instead of a Composition resource"}),"\n",(0,s.jsx)(n.p,{children:"For the first iteration of the switch, an implementation guide will be created and the existing Composition resource referenced in the IG."}),"\n",(0,s.jsx)(n.h2,{id:"sequenced-work-plan",children:"Sequenced Work Plan"}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:["Add an ImplementationGuide that references the existing composition config\n",(0,s.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/issues/3150",children:"GitHub Issue #3150"})]}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Create an IG with at least the fields below:"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"url"})," - globally unique URI for the implementation guide"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"version"})," - sequential version number of the config"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"name"})," - computer friendly name for the IG"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"status"})," - publication status of the IG"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"packageId"})," - package name for the IG"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"fhirVersion"})," - FHIR version(s) the IG targets"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"useContext.valueRange"})," - a range of lowest and highest supported APK version codes. Using the version code over the SEMVER version simplifies filtering by range when fetching from the server"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"definition.resource"})," - a reference to the existing Composition resource"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Update OpenSRP to support syncing using both IG and Composition resources."}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"For apps that do not have an IG, follow the current sync flow using the Composition config"}),"\n",(0,s.jsxs)(n.li,{children:["For apps that have an IG configured:","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Fetch the highest version of the IG config from the server whose useContext range applies for the app\u2019s version."}),"\n",(0,s.jsx)(n.li,{children:"Use the composition config referenced in the IG and follow the standard sync using composition config."}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.li,{children:"In cases where both an IG and a composition config are defined for an app, the IG takes precedence over the composition. The flow in (ii) applies."}),"\n",(0,s.jsx)(n.li,{children:"If both IG and composition resources are not available, the app should fail with a message to the user"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Document how to set IG\u2019s version and useContext range values"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:["The IG ",(0,s.jsx)(n.code,{children:"useContext"})," value should be a range of the app's supported version codes. Using version codes over the app's semantic version allows us to more easily filter from the server by range."]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"useContext.valueRange.low - minimum supported version code. Skip this value if the support starts from the earliest version"}),"\n",(0,s.jsx)(n.li,{children:"useContext.valueRange.high - maximum supported version code. Skip this value if the support starts from low and supports every version above that"}),"\n"]}),"\n",(0,s.jsx)(n.pre,{children:(0,s.jsx)(n.code,{children:'"useContext": [\n  {\n    "valueRange": {\n    "low": {\n        "value": 1\n    },\n    "high": {\n        "value": 10\n    }\n    }\n  }\n]\n'})}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"To ensure there are no missing or improperly referenced configs, and that correct versioning is done, validation will be required in fhir-tooling:"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"FHIR content verification CI tool"}),"\n",(0,s.jsx)(n.li,{children:"Additional checks for missing configs, invalid versioning, etc. to be done when uploading using fhir-tooling"}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:["Display version and useContext info in app\n",(0,s.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/issues/3151",children:"GitHub Issue #3151"})]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Add IG version and useContext values to the application\u2019s settings screen"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:[(0,s.jsx)(n.strong,{children:"[TBD, review with PM/TPM/Dev, requires product owner sign-off]"})," Tag generated content with version of IG. This can be valuable when troubleshooting. Below are some of the considerations to guide the decision on whether to do this"]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Pros","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Useful for debug purposes - provides crucial information during debugging sessions. It allows developers to quickly identify which version of the IG was used to generate specific content, aiding in diagnosing and resolving issues more efficiently. It is also easy to correlate inconsistencies or errors directly to the version of the IG tagged in the resources"}),"\n",(0,s.jsx)(n.li,{children:"Track failure back to version of content - a clear audit trail of content changes and their corresponding IG versions is maintained"}),"\n",(0,s.jsx)(n.li,{children:"Tags can be used to identify resources that need to be migrated in case of an issue tied to a specific version of configs"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Cons","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Increases data size - introduces additional metadata which can slightly increase the overall data size, albeit minimally."}),"\n",(0,s.jsx)(n.li,{children:"Adds code complexity to do tagging"}),"\n",(0,s.jsx)(n.li,{children:"Additional work needs to be done when creating resources to include version information"}),"\n",(0,s.jsx)(n.li,{children:"When a resource was created with an earlier version of configs and edit is done with a newer version, the updated resource may need to be upgraded to include fields and other information added (if any) in the newer version"}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:[(0,s.jsx)(n.strong,{children:"[TBD]"})," Restrict the ability to sync IG based on useContext within version of app doing the syncing eg. get the latest IG version valid for app version"]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["There may be multiple versions of an IG for a given app. How should OpenSRP pick the version of the IG to fetch and use to sync?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Select the most recent version, i.e., IG with highest version number for the given app version"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["How should the app handle cases where a valid IG does not exist for the version of the app?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"The app should provide appropriate feedback to the user, indicating that IG syncing is not available for their current app version. This could be accompanied by instructions on how to update the app to a version that is supported"}),"\n",(0,s.jsx)(n.li,{children:"The app could also offer fallback functionality or access to alternative resources if IG syncing is not possible."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Do the version filters only apply to configs and not to content generated in the app?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Version filters should primarily apply to configs"}),"\n",(0,s.jsx)(n.li,{children:"Content generated within the app may not necessarily be restricted by version filters unless it directly interacts with IG-related functionalities. However, it's essential to ensure that any generated content remains compatible with the selected IG version to maintain data integrity and interoperability."}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.h3,{id:"questions",children:"Questions"}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Why change from a Composition resource to an IG resource?"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"We will have some added LOE to convert from using the Composition to the IG. Do we need to maintain backward compatibility?"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"IGs can be rendered and published - opens up"}),"\n",(0,s.jsx)(n.li,{children:"Latest version of the SDK has a KM"}),"\n"]}),"\n",(0,s.jsx)(n.p,{children:(0,s.jsx)(n.em,{children:"An IG resource will be created for the versions of the app that they support. Resource.reference field of the IG references the exisiting composition resource."})}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"How do we handle bundling of the IG resource using the Workflow manager?"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"An IG is a metadata resource, not a normal resource. It is referenced by a URL rather than an identifier as is the case with composition resources."}),"\n",(0,s.jsxs)(n.li,{children:["How will workflow manager discriminate how if processes a content IG vs Workflow IG?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"The workflow manager should always assume that the IG it is receiving is a workflow IG"}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.li,{children:"Also, they are packaged as maven dependencies? TBD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"IG focuses on bundling workflows as opposed to resources."}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["An app can have multiple IGs that define different workflows.","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"NB: A workflow is a set of resources. For example you can have an antenatal care IG or an Application IG."}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"How does the app know what version of the IG/Composition to load? E.g"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"0.1.0 APK version uses Composition/IG version 2"}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"2.0.0 - 3.0.5 APK version uses Composition/IG version 4"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"The app points to an IG and the IG points to a composition resource"}),"\n",(0,s.jsx)(n.li,{children:"The app should receive the latest IG depending on the range of the version number of the APK."}),"\n"]}),"\n",(0,s.jsx)(n.p,{children:(0,s.jsx)(n.em,{children:"Currently, the composition resource fetched is based on the appId."})}),"\n",(0,s.jsx)(n.p,{children:(0,s.jsx)(n.em,{children:"FHIR spec version R4B does not have an identifier field for IG. This means that we are not able to use the appId to fetch an IG resource. Here are the proposed approaches on how to specify the IG to use:"})}),"\n",(0,s.jsx)(n.p,{children:(0,s.jsx)(n.em,{children:"Configure a URL reference to the IG in the app\u2019s build configurations."})}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Versioning structure to be used"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Questionnaire/001 => Questionnaire/002 - Use 2 files"}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Questionnaire/001/_history/1 => Questionnaire/001/_history/2 - The same file leverage FHIR version"}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:["Does your HAPI FHIR server support versioning using _",(0,s.jsx)(n.em,{children:"history"})]}),"\n",(0,s.jsx)(n.p,{children:"_As highlighted in bullet (c), as not all FHIR servers support resource versioning, the approach to be applied in this case is use of a different identifier for configs with breaking changes.  _"}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"The process of releasing a content IG"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Any content that changes requires a new identifier","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Case 1 - Routine, non-breaking changes to configs that do not require an app update","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Requires no action on the IG"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Case 2 - Changes made to configs require a specific version of the app to work","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"All updated resources/configs are created with new identifiers"}),"\n",(0,s.jsx)(n.li,{children:"A copy of the composition resource is created with a new identifier. References to the changed configs are added to the composition config"}),"\n",(0,s.jsx)(n.li,{children:"A copy of the IG is created, with a different URL to the older version. The new IG resource then references the new composition created in (2) above"}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]})]})}function h(e={}){const{wrapper:n}={...(0,o.R)(),...e.components};return n?(0,s.jsx)(n,{...e,children:(0,s.jsx)(d,{...e})}):d(e)}},8453:(e,n,i)=>{i.d(n,{R:()=>r,x:()=>l});var s=i(6540);const o={},t=s.createContext(o);function r(e){const n=s.useContext(t);return s.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function l(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:r(e.components),s.createElement(t.Provider,{value:n},e.children)}}}]);