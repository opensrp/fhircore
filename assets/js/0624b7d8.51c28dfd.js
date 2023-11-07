"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[714],{9771:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>c,contentTitle:()=>o,default:()=>l,frontMatter:()=>a,metadata:()=>s,toc:()=>d});var i=t(5893),r=t(1151);const a={},o="Refresh/Invalidate cache",s={id:"engineering/android-app/configuring/event-management/refresh-cache",title:"Refresh/Invalidate cache",description:"Event management implementation provides the ability to configure when to invalidate register cache after form submission.",source:"@site/docs/engineering/android-app/configuring/event-management/refresh-cache.mdx",sourceDirName:"engineering/android-app/configuring/event-management",slug:"/engineering/android-app/configuring/event-management/refresh-cache",permalink:"/engineering/android-app/configuring/event-management/refresh-cache",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/docs/engineering/android-app/configuring/event-management/refresh-cache.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Editing",permalink:"/engineering/android-app/configuring/editing"},next:{title:"Resource closure using background workers",permalink:"/engineering/android-app/configuring/event-management/resource-closure-by-background-worker"}},c={},d=[{value:"Sample questionnaire with <code>refreshContent</code>",id:"sample-questionnaire-with-refreshcontent",level:2}];function h(e){const n={code:"code",h1:"h1",h2:"h2",p:"p",pre:"pre",...(0,r.a)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"refreshinvalidate-cache",children:"Refresh/Invalidate cache"}),"\n",(0,i.jsx)(n.p,{children:"Event management implementation provides the ability to configure when to invalidate register cache after form submission."}),"\n",(0,i.jsx)(n.p,{children:"Since we currently cache the register data, we need to refresh/invalidate the cache every time a patient or family is removed from the app. This is to make sure the UI shows the correct data at all times."}),"\n",(0,i.jsxs)(n.p,{children:["The ",(0,i.jsx)(n.code,{children:"refreshContent"})," field in the ",(0,i.jsx)(n.code,{children:"questionnaireConfig"})," determines whether the register cache should be invalidated when a form is submitted."]}),"\n",(0,i.jsxs)(n.h2,{id:"sample-questionnaire-with-refreshcontent",children:["Sample questionnaire with ",(0,i.jsx)(n.code,{children:"refreshContent"})]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-json",children:'       {\n         "title": "Pregnancy Outcome",\n         "titleColor": "@{patientTextColor}",\n         "visible": "@{isPregnant}",\n         "enabled": "@{patientActive}",\n         "actions": [\n           {\n             "trigger": "ON_CLICK",\n             "workflow": "LAUNCH_QUESTIONNAIRE",\n             "questionnaire": {\n               "id": "questionnaire-uuid",\n               "title": "Pregnancy outcome",\n               "resourceIdentifier": "@{patientId}",\n               "planDefinitions": [\n                 "planDefinitions-uuid"\n               ],\n               "refreshContent" : true\n             }\n           }\n         ]\n       }\n'})})]})}function l(e={}){const{wrapper:n}={...(0,r.a)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(h,{...e})}):h(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>s,a:()=>o});var i=t(7294);const r={},a=i.createContext(r);function o(e){const n=i.useContext(a);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function s(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:o(e.components),i.createElement(a.Provider,{value:n},e.children)}}}]);