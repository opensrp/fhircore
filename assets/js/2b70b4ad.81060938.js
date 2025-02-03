"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[6411],{1983:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>s,default:()=>g,frontMatter:()=>o,metadata:()=>d,toc:()=>l});var i=n(4848),r=n(8453);const o={title:"Geowidget"},s="Geowidget configuration",d={id:"engineering/app/configuring/config-types/geowidget",title:"Geowidget",description:"Configurations used to configure map view. FHIR Core uses a mapbox generated UI to position households in a map depending on the location of the household.",source:"@site/docs/engineering/app/configuring/config-types/geowidget.mdx",sourceDirName:"engineering/app/configuring/config-types",slug:"/engineering/app/configuring/config-types/geowidget",permalink:"/engineering/app/configuring/config-types/geowidget",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/app/configuring/config-types/geowidget.mdx",tags:[],version:"current",frontMatter:{title:"Geowidget"},sidebar:"defaultSidebar",previous:{title:"Application",permalink:"/engineering/app/configuring/config-types/application"},next:{title:"Measure reports",permalink:"/engineering/app/configuring/config-types/measure-report"}},c={},l=[{value:"Sample JSON",id:"sample-json",level:2},{value:"Config properties",id:"config-properties",level:2}];function a(e){const t={admonition:"admonition",code:"code",h1:"h1",h2:"h2",header:"header",p:"p",pre:"pre",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",...(0,r.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(t.header,{children:(0,i.jsx)(t.h1,{id:"geowidget-configuration",children:"Geowidget configuration"})}),"\n",(0,i.jsx)(t.p,{children:"Configurations used to configure map view. FHIR Core uses a mapbox generated UI to position households in a map depending on the location of the household."}),"\n",(0,i.jsx)(t.admonition,{type:"info",children:(0,i.jsxs)(t.p,{children:["There can be multiple instances of this configuration type in the application; each should have a unique ",(0,i.jsx)(t.code,{children:"id"}),"."]})}),"\n",(0,i.jsx)(t.h2,{id:"sample-json",children:"Sample JSON"}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-json",children:'{\n    "appId": "map",\n    "id": "householdRegistrationMap",\n    "configType": "geoWidget",\n    "profileId": "householdProfile",\n    "registrationQuestionnaire": {\n        "id": "82952-geowidget",\n        "title": "{{add.family}}",\n        "saveButtonText": "ADD FAMILY",\n        "setPractitionerDetails": true,\n        "setOrganizationDetails": true\n    },\n    "filterDataByLocationLineage": true\n}\n'})}),"\n",(0,i.jsx)(t.h2,{id:"config-properties",children:"Config properties"}),"\n",(0,i.jsxs)(t.table,{children:[(0,i.jsx)(t.thead,{children:(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.th,{children:"Property"}),(0,i.jsx)(t.th,{children:"Description"}),(0,i.jsx)(t.th,{style:{textAlign:"center"},children:"Required"}),(0,i.jsx)(t.th,{style:{textAlign:"center"},children:"Default"})]})}),(0,i.jsxs)(t.tbody,{children:[(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"appId"}),(0,i.jsx)(t.td,{children:"Unique identifier for the application"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(t.td,{style:{textAlign:"center"}})]}),(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"configType"}),(0,i.jsx)(t.td,{children:"Type of configuration"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:(0,i.jsx)(t.code,{children:"geoWidget"})})]}),(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"id"}),(0,i.jsx)(t.td,{children:"A unique identifier for this multi-config type"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(t.td,{style:{textAlign:"center"}})]}),(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"profileId"}),(0,i.jsx)(t.td,{children:"The identifier for the profile to be opened when a point on the map (representing a household) is clicked"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(t.td,{style:{textAlign:"center"}})]}),(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"registrationQuestionnaire"}),(0,i.jsx)(t.td,{children:"Configuration for the register questionnaire"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(t.td,{style:{textAlign:"center"}})]}),(0,i.jsxs)(t.tr,{children:[(0,i.jsx)(t.td,{children:"filterDataByLocationLineage"}),(0,i.jsx)(t.td,{children:"Fetch locations to load on the map using lineage location tags"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(t.td,{style:{textAlign:"center"},children:"false"})]})]})]})]})}function g(e={}){const{wrapper:t}={...(0,r.R)(),...e.components};return t?(0,i.jsx)(t,{...e,children:(0,i.jsx)(a,{...e})}):a(e)}},8453:(e,t,n)=>{n.d(t,{R:()=>s,x:()=>d});var i=n(6540);const r={},o=i.createContext(r);function s(e){const t=i.useContext(o);return i.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function d(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:s(e.components),i.createElement(o.Provider,{value:t},e.children)}}}]);