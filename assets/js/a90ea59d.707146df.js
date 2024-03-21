"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[4047],{9934:(e,n,s)=>{s.r(n),s.d(n,{assets:()=>l,contentTitle:()=>o,default:()=>h,frontMatter:()=>a,metadata:()=>t,toc:()=>d});var i=s(5893),r=s(1151);const a={},o="Releases",t={id:"engineering/android-app/automated-releases",title:"Releases",description:"OpenSRP FHIR Core releases occur at most once every 2 weeks, i.e. at the conclusion of a sprint. The release process follows the gitlab flow style described in the following diagram:",source:"@site/docs/engineering/android-app/automated-releases.mdx",sourceDirName:"engineering/android-app",slug:"/engineering/android-app/automated-releases",permalink:"/engineering/android-app/automated-releases",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/android-app/automated-releases.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Unit Tests",permalink:"/engineering/android-app/developer-setup/testing/unit-tests"},next:{title:"Configurations",permalink:"/engineering/android-app/configuring/"}},l={},d=[{value:"Release notes",id:"release-notes",level:2},{value:"How to write release notes",id:"how-to-write-release-notes",level:3},{value:"Adding release notes to the repository",id:"adding-release-notes-to-the-repository",level:3},{value:"Viewing release notes",id:"viewing-release-notes",level:3},{value:"Code releases",id:"code-releases",level:2},{value:"APK releases",id:"apk-releases",level:2},{value:"Flavors",id:"flavors",level:2},{value:"APK Release artifact via Github workflows",id:"apk-release-artifact-via-github-workflows",level:2}];function c(e){const n={a:"a",blockquote:"blockquote",code:"code",em:"em",h1:"h1",h2:"h2",h3:"h3",img:"img",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",ul:"ul",...(0,r.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"releases",children:"Releases"}),"\n",(0,i.jsxs)(n.p,{children:["OpenSRP FHIR Core releases occur at most once every 2 weeks, i.e. at the conclusion of a sprint. The release process follows the ",(0,i.jsx)(n.a,{href:"https://docs.gitlab.com/ee/topics/gitlab_flow.html#release-branches-with-gitlab-flow",children:"gitlab flow"})," style described in the following diagram:"]}),"\n",(0,i.jsx)(n.p,{children:(0,i.jsx)(n.img,{src:s(9855).Z+"",width:"600",height:"245"})}),"\n",(0,i.jsx)(n.p,{children:"This allows changes to occur on the code release branch while unrelated code continues being merged into main."}),"\n",(0,i.jsx)(n.h2,{id:"release-notes",children:"Release notes"}),"\n",(0,i.jsx)(n.p,{children:"Release notes provide a summary of the changes, improvements, and bug fixes in each new release. They are an essential resource for developers and users to understand what has been updated and any potential impacts on their implementations."}),"\n",(0,i.jsx)(n.h3,{id:"how-to-write-release-notes",children:"How to write release notes"}),"\n",(0,i.jsx)(n.p,{children:"To write effective release notes, follow these guidelines:"}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.strong,{children:"Be concise and clear:"})," Summarize the changes in a way that is easy to understand by users and developers."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.strong,{children:"Categorize changes:"})," Group changes into categories like 'New Features', 'Improvements', and 'Bug Fixes' for easy navigation."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.strong,{children:"Highlight breaking changes:"})," Clearly indicate any breaking changes that may impact existing implementations or require special attention."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.strong,{children:"Include relevant issue numbers:"})," Link to the related GitHub issues or pull requests for more context and easier tracking."]}),"\n"]}),"\n",(0,i.jsx)(n.h3,{id:"adding-release-notes-to-the-repository",children:"Adding release notes to the repository"}),"\n",(0,i.jsxs)(n.p,{children:["Before a new release is created, the release notes must be added to the ",(0,i.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/main/CHANGELOG.md",children:"changelog"}),". The changelog is ordered from newest release at the top to oldest releases at the bottom. Follow these steps to add a changelog entry for your new release:"]}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:["Update, verify, add to the latest changelog section","\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:'If there is an "unreleased" header, update this to the new release version following the document format'}),"\n",(0,i.jsx)(n.li,{children:'If there is no "unreleased" header, add one for this new release version'}),"\n"]}),"\n"]}),"\n",(0,i.jsxs)(n.li,{children:["Update and add to the release notes in this section following the guidelines mentioned above.","\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"At a minimum, the release notes must link to and include the title text from the the newly closed issues, excluding any testing, doc, or cleanup issues that are uninformative or immaterial to the changes."}),"\n"]}),"\n"]}),"\n",(0,i.jsx)(n.li,{children:"Commit and open a PR for your updates to the changelog."}),"\n"]}),"\n",(0,i.jsx)(n.h3,{id:"viewing-release-notes",children:"Viewing release notes"}),"\n",(0,i.jsxs)(n.p,{children:["Release notes can be viewed in the ",(0,i.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/main/CHANGELOG.md",children:"changelog"}),"."]}),"\n",(0,i.jsx)(n.h2,{id:"code-releases",children:"Code releases"}),"\n",(0,i.jsx)(n.p,{children:"To conduct a code release follow the below steps:"}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:["Open and merge a PR to update the project version to the current release version","\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["e.g. change the ",(0,i.jsx)(n.code,{children:"versionName"})," in the ",(0,i.jsx)(n.code,{children:"opensrp"})," module ",(0,i.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/main/android/opensrp/build.gradle#L28",children:"build.gradle"})," file to ",(0,i.jsx)(n.code,{children:"v0.2.0"})]}),"\n"]}),"\n"]}),"\n",(0,i.jsxs)(n.li,{children:["Code freeze: create a branch for the code release version as well as the first code release candidate (labeled RC1).","\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["For example, for code release version ",(0,i.jsx)(n.code,{children:"v0.2.0"}),", create a branch ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0"}),", a pre-release ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0-rc1"}),", and a tag ",(0,i.jsx)(n.code,{children:"v0.2.0-opensrp-rc1"})]}),"\n"]}),"\n"]}),"\n",(0,i.jsxs)(n.li,{children:["Candidate progression: This is followed by 1-2 weeks of QA and error fixing","\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["Whenever we find an error in the candidate, open a pull request with a fix for the error against the code release branch (e.g.  ",(0,i.jsx)(n.code,{children:"v0.2.0"}),")"]}),"\n",(0,i.jsxs)(n.li,{children:["Once the error is fixed and merged into the code release branch (multiple errors can be batched), create a new tag, e.g. ",(0,i.jsx)(n.code,{children:"v0.2.0-opensrp-rc2"}),", the update the pre-release to point to this tag and reflect the RC2 name."]}),"\n"]}),"\n",(0,i.jsxs)(n.blockquote,{children:["\n",(0,i.jsxs)(n.p,{children:["You must also either a) open a PR with the same fix against the ",(0,i.jsx)(n.code,{children:"main"})," branch, b) open a PR from the release branch into the ",(0,i.jsx)(n.code,{children:"main"})," branch after merging the fix into the release branch."]}),"\n"]}),"\n"]}),"\n",(0,i.jsxs)(n.li,{children:["Repeat step (2.) until QA passes, e.g. with more tags, e.g. ",(0,i.jsx)(n.code,{children:"v0.2.0-opensrp-rc3"})," ..., and updated pre-releases."]}),"\n",(0,i.jsxs)(n.li,{children:["Final code release: when the release passes QA, create a final release tag ",(0,i.jsx)(n.code,{children:"v0.2.0"})," and update the release to point to this tag and reflect the ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0"})," name."]}),"\n"]}),"\n",(0,i.jsx)(n.h2,{id:"apk-releases",children:"APK releases"}),"\n",(0,i.jsxs)(n.p,{children:["Once a final code release is created, attach a generic flavor APK release e.g. ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0.apk"})," to the release. In addition, attach APK releases for any specific flavors requested, e.g. ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0-bunda.apk"}),", ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0-ecbis.apk"}),", ",(0,i.jsx)(n.code,{children:"opensrp-0.2.0-wdf.apk"}),"."]}),"\n",(0,i.jsx)(n.h2,{id:"flavors",children:"Flavors"}),"\n",(0,i.jsx)(n.p,{children:"Flavors define custom names, icons, and default local properties (such as server versions). We use flavors when a particular project or use-cases requires this customization, such as a branded icon and name. When creating flavors do NOT include version numbers. The version of the flavor is defined by the code version."}),"\n",(0,i.jsxs)(n.p,{children:["To add a flavor, add an entry to the ",(0,i.jsx)(n.code,{children:"productFlavors"})," map in ",(0,i.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/main/android/opensrp/build.gradle",children:(0,i.jsx)(n.code,{children:"android/opensrp/build.gradle"})}),". For example, to add a flavor called ",(0,i.jsx)(n.code,{children:"newFlavor"})," add the map:"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'create("newFlavor") {\n    dimension = "apps"\n    applicationIdSuffix = ".new-flavor"\n    versionNameSuffix = "-new-flavor"\n    manifestPlaceholders["appLabel"] = "New Flavor App Name"\n}\n'})}),"\n",(0,i.jsx)(n.p,{children:"Note, flavor names should always be camel cased"}),"\n",(0,i.jsx)(n.p,{children:"You can add the following resources:"}),"\n",(0,i.jsxs)(n.table,{children:[(0,i.jsx)(n.thead,{children:(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.th,{children:"Description"}),(0,i.jsx)(n.th,{children:"Location"})]})}),(0,i.jsx)(n.tbody,{children:(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"App icon"}),(0,i.jsx)(n.td,{children:(0,i.jsx)(n.code,{children:"android/opensrp/src/new-flavor/res/drawable/ic_app_logo.png"})})]})})]}),"\n",(0,i.jsx)(n.p,{children:"Where the app icon is an image of the appropriate size"}),"\n",(0,i.jsx)(n.h2,{id:"apk-release-artifact-via-github-workflows",children:"APK Release artifact via Github workflows"}),"\n",(0,i.jsx)(n.p,{children:"As part of integrating Continuous Delivery(CD) into the development lifecycle, CI is set up to generate an APK."}),"\n",(0,i.jsx)(n.p,{children:"To Generate an APK or AAB artifact from an existing snapshot of the codebase click on the Github actions tab."}),"\n",(0,i.jsxs)(n.p,{children:["On the left panel select the Manual APK Release workflow.\n",(0,i.jsx)(n.img,{src:s(8081).Z+"",width:"1054",height:"60"})]}),"\n",(0,i.jsxs)(n.p,{children:["Click on ",(0,i.jsx)(n.em,{children:"Run Workflow"}),"\n",(0,i.jsx)(n.img,{src:s(2288).Z+"",width:"659",height:"150"})]}),"\n",(0,i.jsxs)(n.p,{children:["This pops up the dialog below where you need to provide inputs specific to your release.\n",(0,i.jsx)(n.img,{src:s(7497).Z+"",width:"356",height:"362"})]}),"\n",(0,i.jsx)(n.p,{children:"Input required:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["Select the snapshot of the codebase from either a ",(0,i.jsx)(n.em,{children:"branch"})," or a ",(0,i.jsx)(n.em,{children:"tag"}),"."]}),"\n",(0,i.jsx)(n.li,{children:"Select the build variant e.g. Release vs Debug variant"}),"\n",(0,i.jsxs)(n.li,{children:["Enter the exact name of the the flavor e.g. sidBunda, zeir . ",(0,i.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/025125106b5467c26d32de9b60387429d0808548/android/quest/build.gradle.kts#L182",children:"See current list of flavors here"}),"."]}),"\n",(0,i.jsxs)(n.li,{children:["If you need an ",(0,i.jsx)(n.em,{children:"AAB"})," artifact check the box otherwise leave it unchecked to generate an ",(0,i.jsx)(n.em,{children:"APK"})," artifact."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["Click on the ",(0,i.jsx)(n.em,{children:"Run Workflow"})," button. This will trigger the action to run."]}),"\n",(0,i.jsxs)(n.p,{children:["After the action run is complete click on the last workflow run link.\n",(0,i.jsx)(n.img,{src:s(615).Z+"",width:"323",height:"81"})]}),"\n",(0,i.jsxs)(n.p,{children:["Scroll to the bottom to find the generated artifact download link. Click to download.\n",(0,i.jsx)(n.img,{src:s(1826).Z+"",width:"353",height:"149"})]})]})}function h(e={}){const{wrapper:n}={...(0,r.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(c,{...e})}):c(e)}},9855:(e,n,s)=>{s.d(n,{Z:()=>i});const i=s.p+"assets/images/gitlabflow-d004f34eb8bed99cce0dade9bb835d70.png"},7497:(e,n,s)=>{s.d(n,{Z:()=>i});const i=s.p+"assets/images/manual-workflow-input-popup-c1ef5b4a6cb86ecf66a81a39554c7382.png"},8081:(e,n,s)=>{s.d(n,{Z:()=>i});const i=s.p+"assets/images/manual-workflow-menu-item-e4e7365f0478dbf0c8226f69d2cfaeb7.png"},1826:(e,n,s)=>{s.d(n,{Z:()=>i});const i=s.p+"assets/images/manual-workflow-release-artifact-link-09efc326f86d05178690f3d731cdd7a5.png"},2288:(e,n,s)=>{s.d(n,{Z:()=>i});const i=s.p+"assets/images/manual-workflow-run-button-9a1c99072394c596ecd21ce9d0fc73ec.png"},615:(e,n,s)=>{s.d(n,{Z:()=>i});const i="data:image/png;base64,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"},1151:(e,n,s)=>{s.d(n,{Z:()=>t,a:()=>o});var i=s(7294);const r={},a=i.createContext(r);function o(e){const n=i.useContext(a);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function t(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:o(e.components),i.createElement(a.Provider,{value:n},e.children)}}}]);