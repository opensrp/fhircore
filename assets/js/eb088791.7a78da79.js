"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[2545],{7184:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>o,contentTitle:()=>c,default:()=>h,frontMatter:()=>s,metadata:()=>l,toc:()=>d});var i=t(5893),r=t(1151);const s={title:"Application"},c="Application configuration",l={id:"engineering/android-app/configuring/config-types/application",title:"Application",description:"These are app wide configurations used to control the application behaviour globally e.g. application theme, app language etc.",source:"@site/docs/engineering/android-app/configuring/config-types/application.mdx",sourceDirName:"engineering/android-app/configuring/config-types",slug:"/engineering/android-app/configuring/config-types/application",permalink:"/engineering/android-app/configuring/config-types/application",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/docs/engineering/android-app/configuring/config-types/application.mdx",tags:[],version:"current",frontMatter:{title:"Application"},sidebar:"defaultSidebar",previous:{title:"App Flavors",permalink:"/engineering/android-app/configuring/add-application-flavors"},next:{title:"Geowidget",permalink:"/engineering/android-app/configuring/config-types/geowidget"}},o={},d=[{value:"Sample JSON",id:"sample-json",level:2},{value:"Config properties",id:"config-properties",level:2}];function a(e){const n={a:"a",admonition:"admonition",code:"code",h1:"h1",h2:"h2",p:"p",pre:"pre",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",...(0,r.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"application-configuration",children:"Application configuration"}),"\n",(0,i.jsx)(n.p,{children:"These are app wide configurations used to control the application behaviour globally e.g. application theme, app language etc."}),"\n",(0,i.jsx)(n.admonition,{type:"note",children:(0,i.jsxs)(n.p,{children:["There can only be one instance of application configuration for the entire application. There are instances where the ",(0,i.jsx)(n.code,{children:"Event Workflow"})," is added to the application config. See ",(0,i.jsx)(n.a,{href:"https://docs.opensrp.io/engineering/android-app/configuring/event-management/resource-closure-by-background-worker",children:"here"})]})}),"\n",(0,i.jsx)(n.h2,{id:"sample-json",children:"Sample JSON"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-json",children:'{\n    "appId": "app",\n    "configType": "application",\n    "appTitle": "eCBIS (CHA & CHSS)",\n    "remoteSyncPageSize": 100,\n    "languages": [\n        "en"\n    ],\n    "useDarkTheme": false,\n    "syncInterval": 15,\n    "syncStrategy": [\n        "Location",\n        "Organization",\n        "CareTeam",\n        "Practitioner"\n    ],\n    "loginConfig": {\n        "showLogo": true,\n        "enablePin": true,\n        "showAppTitle" : true,\n        "logoHeight" : 120,\n        "logoWidth" : 140\n    },\n    "deviceToDeviceSync": {\n        "resourcesToSync": [\n            "Group",\n            "Patient",\n            "CarePlan",\n            "Task",\n            "Encounter",\n            "Observation",\n            "Condition",\n            "Questionnaire",\n            "QuestionnaireResponse"\n        ]\n    }\n}\n'})}),"\n",(0,i.jsx)(n.h2,{id:"config-properties",children:"Config properties"}),"\n",(0,i.jsxs)(n.table,{children:[(0,i.jsx)(n.thead,{children:(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.th,{children:"Property"}),(0,i.jsx)(n.th,{children:"Description"}),(0,i.jsx)(n.th,{style:{textAlign:"center"},children:"Required"}),(0,i.jsx)(n.th,{style:{textAlign:"center"},children:"Default"})]})}),(0,i.jsxs)(n.tbody,{children:[(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"appId"}),(0,i.jsx)(n.td,{children:"Unique identifier for the application"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"}})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"configType"}),(0,i.jsx)(n.td,{children:"Type of configuration"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"application"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"appTitle"}),(0,i.jsx)(n.td,{children:"Name of the application displayed on side menu (drawer)"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:'""'})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"remoteSyncPageSize"}),(0,i.jsx)(n.td,{children:"Sync batch size"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"100"})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"languages"}),(0,i.jsx)(n.td,{children:"Supported languages"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"['en']"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"useDarkTheme"}),(0,i.jsx)(n.td,{children:"Indicate whether to apply dark theme"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"false"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"syncInterval"}),(0,i.jsx)(n.td,{children:"Configuration duration for periodic sync"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"30"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"syncStrategy"}),(0,i.jsx)(n.td,{children:"Tag every resource with the values for the resource types indicated here"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"emptyList()"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"loginConfig.showLogo"}),(0,i.jsx)(n.td,{children:"Display logo in login page"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"Yes"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"true"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"loginConfig.enablePin"}),(0,i.jsx)(n.td,{children:"Request user for pin after login; to be used for subsequent logins"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"false"})})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"loginConfig.logoHeight"}),(0,i.jsx)(n.td,{children:"Set the maximum height a logo can have"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"120"})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"loginConfig.logoWidth"}),(0,i.jsx)(n.td,{children:"Set the maximum width a logo can have"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"140"})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"loginConfig.showAppTitle"}),(0,i.jsx)(n.td,{children:"Toggle App title in LoginScreen visibility"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"true"})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"deviceToDeviceSync.resourcesToSync"}),(0,i.jsx)(n.td,{children:"Types of resource to be synced from one device to another during peer connection"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:"No"}),(0,i.jsx)(n.td,{style:{textAlign:"center"},children:(0,i.jsx)(n.code,{children:"false"})})]})]})]})]})}function h(e={}){const{wrapper:n}={...(0,r.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(a,{...e})}):a(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>l,a:()=>c});var i=t(7294);const r={},s=i.createContext(r);function c(e){const n=i.useContext(s);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function l(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:c(e.components),i.createElement(s.Provider,{value:n},e.children)}}}]);