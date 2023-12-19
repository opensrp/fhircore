"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[8226],{4458:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>a,contentTitle:()=>s,default:()=>p,frontMatter:()=>o,metadata:()=>d,toc:()=>l});var r=t(5893),i=t(1151);const o={},s="Sentry setup",d={id:"engineering/android-app/developer-setup/sentry",title:"Sentry setup",description:"### Note",source:"@site/docs/engineering/android-app/developer-setup/sentry.mdx",sourceDirName:"engineering/android-app/developer-setup",slug:"/engineering/android-app/developer-setup/sentry",permalink:"/engineering/android-app/developer-setup/sentry",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/docs/engineering/android-app/developer-setup/sentry.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Publishing SDK Artifacts",permalink:"/engineering/android-app/developer-setup/publishing-fhir-sdk-artifacts"},next:{title:"Testing",permalink:"/engineering/android-app/developer-setup/testing/"}},a={},l=[{value:"Note",id:"note",level:3},{value:"Instructions",id:"instructions",level:3}];function c(e){const n={a:"a",blockquote:"blockquote",code:"code",h1:"h1",h3:"h3",img:"img",li:"li",ol:"ol",p:"p",ul:"ul",...(0,i.a)(),...e.components};return(0,r.jsxs)(r.Fragment,{children:[(0,r.jsx)(n.h1,{id:"sentry-setup",children:"Sentry setup"}),"\n",(0,r.jsxs)(n.blockquote,{children:["\n",(0,r.jsx)(n.h3,{id:"note",children:"Note"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["You need to have already setup a ",(0,r.jsx)(n.a,{href:"https://docs.sentry.io/product/sentry-basics/integrate-frontend/create-new-project/",children:"project"})," and dashboard with the necessary access permissions, and"]}),"\n",(0,r.jsxs)(n.li,{children:["are able to obtain the dsn (Data Source Name) of this project through\xa0",(0,r.jsx)(n.code,{children:"[Project]\xa0> Settings > Client Keys (DSN)"}),". This will be required later for configuration and logging."]}),"\n"]}),"\n"]}),"\n",(0,r.jsx)(n.h3,{id:"instructions",children:"Instructions"}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Once you have created the Sentry project as instructed above, inside the ",(0,r.jsx)(n.code,{children:"local.properties"})," file add a line in the form\n",(0,r.jsx)(n.code,{children:"SENTRY_DSN=xxxxxx"})," replace the ",(0,r.jsx)(n.code,{children:"xxxxxx"})," with the ",(0,r.jsx)(n.code,{children:"sentry dsn"})," from your project in Sentry. This will be picked up\nautomatically by the setup inside the quest module's build.gradle file"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["If you want to seperate your staging and production environment logs and performance information you can add an environment variable\nto the Sentry,init method. You will need to insert an extra option in the form of ",(0,r.jsx)(n.code,{children:"options.environment = <Your Environment Name>"}),". When\ndata gets pushed to the dashboard for the first time, the environment will be automatically created. Use the\xa0Environment tag set from the previous step to filter               environment as shown below.\n",(0,r.jsx)(n.img,{src:"https://github.com/opensrp/fhircore/assets/12864384/e00e9da9-01ea-41ec-aa72-5588fde4fdf8",alt:"sentry_environment_dropdown"})]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"Timber logs should show up in the Sentry dashboard. Sentry will track crashes and timber Errors. You can also\nconfigure the log levels inside the Sentry.init function"}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"By default we have configured additional Sentry integrations for:"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsx)(n.li,{children:"Navigation"}),"\n",(0,r.jsx)(n.li,{children:"Fragments"}),"\n"]}),"\n",(0,r.jsxs)(n.p,{children:["You can also add support for ",(0,r.jsx)(n.code,{children:"Jetpack compose"})," and ",(0,r.jsx)(n.code,{children:"OkHttp"})]}),"\n"]}),"\n"]})]})}function p(e={}){const{wrapper:n}={...(0,i.a)(),...e.components};return n?(0,r.jsx)(n,{...e,children:(0,r.jsx)(c,{...e})}):c(e)}},1151:(e,n,t)=>{t.d(n,{Z:()=>d,a:()=>s});var r=t(7294);const i={},o=r.createContext(i);function s(e){const n=r.useContext(o);return r.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function d(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:s(e.components),r.createElement(o.Provider,{value:n},e.children)}}}]);