"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[9859],{4918:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>a,default:()=>h,frontMatter:()=>r,metadata:()=>o,toc:()=>d});var s=n(4848),i=n(8453);const r={},a="Testing",o={id:"engineering/testing/readme",title:"Testing",description:"The OpenSRP Android app includes automated style, coverage, unit, user inteface / integartion, and performance testing. All tests are run through github actions on pull request and must be passed for a pull request to be merged without an admin override.",source:"@site/docs/engineering/testing/readme.mdx",sourceDirName:"engineering/testing",slug:"/engineering/testing/",permalink:"/engineering/testing/",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/testing/readme.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"DHIS2",permalink:"/engineering/integrations/dhis2"},next:{title:"Performance tests",permalink:"/engineering/testing/performance-tests"}},c={},d=[{value:"Style and coverage tests",id:"style-and-coverage-tests",level:2},{value:"Unit tests",id:"unit-tests",level:2},{value:"User interface and integration tests",id:"user-interface-and-integration-tests",level:2},{value:"Performance tests",id:"performance-tests",level:2},{value:"Device compatibility tests",id:"device-compatibility-tests",level:2},{value:"Volume testing",id:"volume-testing",level:2}];function l(e){const t={a:"a",h1:"h1",h2:"h2",header:"header",p:"p",...(0,i.R)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsx)(t.header,{children:(0,s.jsx)(t.h1,{id:"testing",children:"Testing"})}),"\n",(0,s.jsx)(t.p,{children:"The OpenSRP Android app includes automated style, coverage, unit, user inteface / integartion, and performance testing. All tests are run through github actions on pull request and must be passed for a pull request to be merged without an admin override."}),"\n",(0,s.jsx)(t.h2,{id:"style-and-coverage-tests",children:"Style and coverage tests"}),"\n",(0,s.jsxs)(t.p,{children:["We use ",(0,s.jsx)(t.a,{href:"https://github.com/pinterest/ktlint",children:"ktlint"})," via ",(0,s.jsx)(t.a,{href:"https://github.com/diffplug/spotless",children:"spotless"})," to run style checks against the entire codebase. We target to make these style checks as strict as possible in order to reduce bikeshedding in code reviews."]}),"\n",(0,s.jsxs)(t.p,{children:["We use ",(0,s.jsx)(t.a,{href:"https://www.jacoco.org/jacoco/",children:"Jacoco"})," for code coverage report generation and then ",(0,s.jsx)(t.a,{href:"https://app.codecov.io/gh/opensrp/fhircore",children:"codecov"})," to track changes in coverage over time. We enforce a minimum coverage percent on the new code added in a pull request and a minimum reduction in overall coverage percentage change when consider the changes introduced through a pull request."]}),"\n",(0,s.jsx)(t.h2,{id:"unit-tests",children:"Unit tests"}),"\n",(0,s.jsxs)(t.p,{children:["Unit tests are divided among the ",(0,s.jsx)(t.a,{href:"https://github.com/opensrp/fhircore/tree/main/android/engine/src/test",children:"engine"}),", ",(0,s.jsx)(t.a,{href:"https://github.com/opensrp/fhircore/tree/main/android/geowidget/src/test/java/org/smartregister/fhircore/geowidget",children:"geowidget"}),", and ",(0,s.jsx)(t.a,{href:"https://github.com/opensrp/fhircore/tree/main/android/quest/src/test",children:"quest"})," modules. These can be run locally and are run automatically through github actions when you submit a pull request. All tests must pass for a pull request to be merged."]}),"\n",(0,s.jsx)(t.h2,{id:"user-interface-and-integration-tests",children:"User interface and integration tests"}),"\n",(0,s.jsxs)(t.p,{children:["We run tests against screen renderings that function as user interface and integations tests. These are defined in the ",(0,s.jsx)(t.a,{href:"https://github.com/opensrp/fhircore/tree/main/android/quest/src/test",children:"quest"})," module. These can be run locally and are run automatically through github actions when you submit a pull request. All tests must pass for a pull request to be merged.\nIn addition, we conduct manual tests to accommodate all functionalities and E2E user journeys to include all he steps a user interacts with."]}),"\n",(0,s.jsx)(t.h2,{id:"performance-tests",children:"Performance tests"}),"\n",(0,s.jsxs)(t.p,{children:["We include a set of ",(0,s.jsx)(t.a,{href:"https://github.com/opensrp/fhircore/tree/main/android/quest/src/androidTest/java/org/smartregister/fhircore/performance",children:"performance tests"})," to verify that the time taken to perform operations is not changing significantly as the code changes. These measure relative performance when running on the hosted continuous integration testing system and are not meant to reflect the amount of time an operation takes in a real world on-device scenario."]}),"\n",(0,s.jsx)(t.h2,{id:"device-compatibility-tests",children:"Device compatibility tests"}),"\n",(0,s.jsx)(t.p,{children:"We run the applications in different devices with different Android versions, RAM and ROM in order to determine the least specifications the application can run on. This key in guiding users."}),"\n",(0,s.jsx)(t.h2,{id:"volume-testing",children:"Volume testing"}),"\n",(0,s.jsx)(t.p,{children:"We usually insert massive volume of loads of data in order to check the application behavior."})]})}function h(e={}){const{wrapper:t}={...(0,i.R)(),...e.components};return t?(0,s.jsx)(t,{...e,children:(0,s.jsx)(l,{...e})}):l(e)}},8453:(e,t,n)=>{n.d(t,{R:()=>a,x:()=>o});var s=n(6540);const i={},r=s.createContext(i);function a(e){const t=s.useContext(r);return s.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function o(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:a(e.components),s.createElement(r.Provider,{value:t},e.children)}}}]);