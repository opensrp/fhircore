"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[8700],{9511:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>a,contentTitle:()=>r,default:()=>h,frontMatter:()=>o,metadata:()=>c,toc:()=>l});var s=i(5893),t=i(1151);const o={},r="Bundling Configurations to Target Specific App Versions",c={id:"engineering/android-app/system-design/configs-versioning",title:"Bundling Configurations to Target Specific App Versions",description:"|||",source:"@site/docs/engineering/android-app/system-design/configs-versioning.md",sourceDirName:"engineering/android-app/system-design",slug:"/engineering/android-app/system-design/configs-versioning",permalink:"/engineering/android-app/system-design/configs-versioning",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/android-app/system-design/configs-versioning.md",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Tagging Resources",permalink:"/engineering/android-app/datastore/tagging"},next:{title:"Backend",permalink:"/engineering/backend/"}},a={},l=[{value:"Background",id:"background",level:2},{value:"Switch from Composition to ImplementationGuide",id:"switch-from-composition-to-implementationguide",level:2},{value:"Sequenced Work Plan",id:"sequenced-work-plan",level:2}];function d(e){const n={code:"code",h1:"h1",h2:"h2",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",ul:"ul",...(0,t.a)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsx)(n.h1,{id:"bundling-configurations-to-target-specific-app-versions",children:"Bundling Configurations to Target Specific App Versions"}),"\n",(0,s.jsxs)(n.table,{children:[(0,s.jsx)(n.thead,{children:(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.th,{}),(0,s.jsx)(n.th,{})]})}),(0,s.jsxs)(n.tbody,{children:[(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Date Submitted"}),(0,s.jsx)(n.td,{children:"March 7, 2024"})]}),(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Date Approved"}),(0,s.jsx)(n.td,{children:"TBD"})]}),(0,s.jsxs)(n.tr,{children:[(0,s.jsx)(n.td,{children:"Status"}),(0,s.jsx)(n.td,{children:"In review"})]})]})]}),"\n",(0,s.jsx)(n.h2,{id:"background",children:"Background"}),"\n",(0,s.jsx)(n.p,{children:"There is a need to ensure compatibility between FHIR configs downloaded from the server and the version of OpenSRP app. With OpenSRP still in active development, with ongoing changes to how configs are defined, implementing version-based content limitations is crucial to ensuring that the application functions correctly. This allows for streamlining of user experience and maintaining consistency across different versions of the application."}),"\n",(0,s.jsx)(n.h2,{id:"switch-from-composition-to-implementationguide",children:"Switch from Composition to ImplementationGuide"}),"\n",(0,s.jsx)(n.p,{children:"OpenSRP currently uses a composition resource to define resources that map out a OpenSRP application. Storing versioning information in the composition resource is non-trivial."}),"\n",(0,s.jsx)(n.p,{children:"An ImplementationGuide (IG) is designed with versioning support and rich metadata such as licensing information, authors, publication status, etc."}),"\n",(0,s.jsxs)(n.p,{children:["An IG has a ",(0,s.jsx)(n.code,{children:"useContext"})," field whose data type is a ",(0,s.jsx)(n.code,{children:"UsageContext"})," that has ",(0,s.jsx)(n.code,{children:"Range"})," as one of the allowed types. Range has ",(0,s.jsx)(n.code,{children:"low"})," and ",(0,s.jsx)(n.code,{children:"high"})," values which can be used to set the app\u2019s version range supported by the configs. It also has a ",(0,s.jsx)(n.code,{children:"version"})," field of the content."]}),"\n",(0,s.jsxs)(n.p,{children:["The ",(0,s.jsx)(n.code,{children:"useContext.valueRange"})," defines the lowest and highest APK versions it is compatible with."]}),"\n",(0,s.jsxs)(n.p,{children:["IG\u2019s ",(0,s.jsx)(n.code,{children:"definition"})," field maps to ",(0,s.jsx)(n.code,{children:"section"})," field of the composition."]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"resource.reference"})," maps to ",(0,s.jsx)(n.code,{children:"section.focus.reference"})]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"resource.name"})," maps to ",(0,s.jsx)(n.code,{children:"section.focus.indentifier.value"})]}),"\n"]}),"\n",(0,s.jsx)(n.p,{children:"ImplementationGuide"}),"\n",(0,s.jsx)(n.pre,{children:(0,s.jsx)(n.code,{children:"  - version\n  - useContext (valueRange)\n    - low\n    - high\n  - definition\n    - resource\n      - reference\n      - name\n"})}),"\n",(0,s.jsx)(n.p,{children:"ImplementationGuides are used to package all related resources to manage workflows e.g. immunization IG, malaria IG, HIV IG, etc. To align with how others use IGs, the ideal approach inOpenSRP would be to link all resources referenced in the composition config\u2019s section in the implementation guide and fully switch to using an IG instead of a composition resource."}),"\n",(0,s.jsx)(n.p,{children:"For the first iteration of the switch, an implementation guide will be created and the existing composition config referenced in the IG."}),"\n",(0,s.jsx)(n.h2,{id:"sequenced-work-plan",children:"Sequenced Work Plan"}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Add an ImplementationGuide that references the existing composition config"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Create an IG with at least the fields below:","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"version"})," - sequential version number of the config"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"useContext.valueRange"})," - a range of lowest and highest supported APK versions of the app"]}),"\n",(0,s.jsxs)(n.li,{children:[(0,s.jsx)(n.code,{children:"definition.resource"})," - a reference to the existing composition resource"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Update OpenSRP to support syncing using both IG and composition configs","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"For apps that do not have an IG, follow the current sync flow using the composition config"}),"\n",(0,s.jsxs)(n.li,{children:["For apps that have an IG configured:","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Fetch the highest version of the IG config from the server whose useContext range applies for the app\u2019s version."}),"\n",(0,s.jsx)(n.li,{children:"Use the composition config referenced in the IG and follow the standard sync using composition config."}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.li,{children:"In cases where both an IG and a composition config are defined for an app, the IG takes precedence over the composition. The flow in (ii) applies."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Document how to set IG\u2019s ",(0,s.jsx)(n.code,{children:"version"}),", ",(0,s.jsx)(n.code,{children:"useContext"}),", etc. follow SEMVER etc","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["To ensure there are no missing or improperly referenced configs, and that correct versioning is done, validation will be required in fhir-tooling:","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"FHIR content verification CI tool"}),"\n",(0,s.jsx)(n.li,{children:"Additional checks for missing configs, invalid versioning, etc. to be done when uploading using fhir-tooling"}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsx)(n.p,{children:"Display version and useContext info in app"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Add IG version and useContext values to the application\u2019s settings screen"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:[(0,s.jsx)(n.strong,{children:"[TBD, review with PM/TPM/Dev, requires product owner sign-off]"})," Tag generated content with version of IG. This can be valuable when troubleshooting. Below are some of the considerations to guide the decision on whether to do this"]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Pros","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Useful for debug purposes - provides crucial information during debugging sessions. It allows developers to quickly identify which version of the IG was used to generate specific content, aiding in diagnosing and resolving issues more efficiently. It is also easy to correlate inconsistencies or errors directly to the version of the IG tagged in the resources"}),"\n",(0,s.jsx)(n.li,{children:"Track failure back to version of content - a clear audit trail of content changes and their corresponding IG versions is maintained"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Cons","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Increases data size - introduces additional metadata which can slightly increase the overall data size, albeit minimally."}),"\n",(0,s.jsx)(n.li,{children:"Adds code complexity to do tagging"}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["\n",(0,s.jsxs)(n.p,{children:[(0,s.jsx)(n.strong,{children:"[TBD]"})," Restrict the ability to sync IG based on useContext within version of app doing the syncing eg. get the latest IG version valid for app version"]}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["There may be multiple versions of an IG for a given app. How should OpenSRP pick the version of the IG to fetch and use to sync?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Select the most recent version, i.e., IG with highest version number for the given app version"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["How should the app handle cases where a valid IG does not exist for the version of the app?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"The app should provide appropriate feedback to the user, indicating that IG syncing is not available for their current app version. This could be accompanied by instructions on how to update the app to a version that is supported"}),"\n",(0,s.jsx)(n.li,{children:"The app could also offer fallback functionality or access to alternative resources if IG syncing is not possible."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Do the version filters only apply to configs and not to content generated in the app?","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Version filters should primarily apply to configs"}),"\n",(0,s.jsx)(n.li,{children:"Content generated within the app may not necessarily be restricted by version filters unless it directly interacts with IG-related functionalities. However, it's essential to ensure that any generated content remains compatible with the selected IG version to maintain data integrity and interoperability."}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]})]})}function h(e={}){const{wrapper:n}={...(0,t.a)(),...e.components};return n?(0,s.jsx)(n,{...e,children:(0,s.jsx)(d,{...e})}):d(e)}},1151:(e,n,i)=>{i.d(n,{Z:()=>c,a:()=>r});var s=i(7294);const t={},o=s.createContext(t);function r(e){const n=s.useContext(o);return s.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(t):e.components||t:r(e.components),s.createElement(o.Provider,{value:n},e.children)}}}]);