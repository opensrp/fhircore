"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[5409],{9020:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>h,contentTitle:()=>r,default:()=>l,frontMatter:()=>o,metadata:()=>s,toc:()=>c});var a=n(4848),i=n(8453);const o={sidebar_label:"FHIR Gateway"},r="FHIR Information Gateway",s={id:"engineering/backend/info-gateway/readme",title:"FHIR Information Gateway",description:"How it works",source:"@site/docs/engineering/backend/info-gateway/readme.mdx",sourceDirName:"engineering/backend/info-gateway",slug:"/engineering/backend/info-gateway/",permalink:"/engineering/backend/info-gateway/",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/backend/info-gateway/readme.mdx",tags:[],version:"current",frontMatter:{sidebar_label:"FHIR Gateway"},sidebar:"defaultSidebar",previous:{title:"Identity Access Management",permalink:"/engineering/backend/iam"},next:{title:"Sync Strategies",permalink:"/engineering/backend/info-gateway/sync-strategies"}},h={},c=[{value:"How it works",id:"how-it-works",level:3},{value:"Filtering FHIR API data based on meta tags",id:"filtering-fhir-api-data-based-on-meta-tags",level:3},{value:"How to set up the FHIR Gateway host",id:"how-to-set-up-the-fhir-gateway-host",level:3}];function d(e){const t={a:"a",blockquote:"blockquote",h1:"h1",h3:"h3",header:"header",li:"li",p:"p",strong:"strong",ul:"ul",...(0,i.R)(),...e.components};return(0,a.jsxs)(a.Fragment,{children:[(0,a.jsx)(t.header,{children:(0,a.jsx)(t.h1,{id:"fhir-information-gateway",children:"FHIR Information Gateway"})}),"\n",(0,a.jsx)(t.h3,{id:"how-it-works",children:"How it works"}),"\n",(0,a.jsx)(t.p,{children:"The FHIR Information Gateway is a proxy that sits between the clients and the FHIR API. This allows us to consistently handle authorization agnostic to the system that happens to be providing the FHIR API we are fetching data from, i.e.  the client will connect to the FHIR Information Gateway the same way regardless of whether the underlying FHIR API is being provided HAPI FHIR, Google Cloud Healthcare API, Azure Health Data Service, or anything else."}),"\n",(0,a.jsx)(t.p,{children:"When using HAPI as the FHIR API, after the FHIR Information Gateway is deployed, the HAPI FHIR backend is deployed with the integrated Keycloak configuration disabled. Any requests made to the backend by the client are now made to the FHIR Information Gateway, which then proxies the request to the HAPI FHIR API and only allows access to the API endpoints if the token provided by the client has the relevant authorization."}),"\n",(0,a.jsxs)(t.blockquote,{children:["\n",(0,a.jsxs)(t.p,{children:[(0,a.jsx)(t.strong,{children:"Note"}),": In a production environment the FHIR API and data store, e.g. HAPI FHIR backend, would be inaccessible to the public and only accessible from the IP of the FHIR Information Gateway or via a VPN."]}),"\n"]}),"\n",(0,a.jsx)(t.p,{children:"We have written a set of plugins that extend the FHIR Information Gateway functionality to provide features useful to OpenSRP. This includes the following plugins:"}),"\n",(0,a.jsxs)(t.ul,{children:["\n",(0,a.jsxs)(t.li,{children:["\n",(0,a.jsxs)(t.p,{children:[(0,a.jsx)(t.strong,{children:"Permissions Checker"})," - Authorization per FHIR Endpoint per HTTP Verb"]}),"\n"]}),"\n",(0,a.jsxs)(t.li,{children:["\n",(0,a.jsxs)(t.p,{children:[(0,a.jsx)(t.strong,{children:"Data Access Checker"})," - Data filtering based on user assignment, i.e. filtering by Organization, Location, Practitioner, or CareTeam"]}),"\n"]}),"\n",(0,a.jsxs)(t.li,{children:["\n",(0,a.jsxs)(t.p,{children:[(0,a.jsx)(t.strong,{children:"Data Requesting"})," - Data fetching mechanism for FHIR Resources defining patient data vs OpenSRP 2.0 application sync config resources"]}),"\n"]}),"\n"]}),"\n",(0,a.jsx)(t.h3,{id:"filtering-fhir-api-data-based-on-meta-tags",children:"Filtering FHIR API data based on meta tags"}),"\n",(0,a.jsx)(t.p,{children:"The OpenSRP 2.0  client application has logic that tags all the resources created with meta tags that correspond to the supported sync strategies i.e. Organization, Location, Practitioner, and CareTeam. This way, if we need to change a sync strategy for a deployment or support different strategies for various roles we can change their sync strategy and the relevant data would be downloaded since it is already tagged."}),"\n",(0,a.jsx)(t.h3,{id:"how-to-set-up-the-fhir-gateway-host",children:"How to set up the FHIR Gateway host"}),"\n",(0,a.jsx)(t.p,{children:"The Gateway setup and configuration is documented here:"}),"\n",(0,a.jsxs)(t.ul,{children:["\n",(0,a.jsxs)(t.li,{children:["\n",(0,a.jsx)(t.p,{children:(0,a.jsx)(t.a,{href:"https://github.com/google/fhir-gateway",children:"FHIR Gateway Setup and configuration"})}),"\n"]}),"\n",(0,a.jsxs)(t.li,{children:["\n",(0,a.jsx)(t.p,{children:(0,a.jsx)(t.a,{href:"https://hub.docker.com/r/opensrp/fhir-gateway/tags",children:"FHIR Gateway Docker image"})}),"\n"]}),"\n"]})]})}function l(e={}){const{wrapper:t}={...(0,i.R)(),...e.components};return t?(0,a.jsx)(t,{...e,children:(0,a.jsx)(d,{...e})}):d(e)}},8453:(e,t,n)=>{n.d(t,{R:()=>r,x:()=>s});var a=n(6540);const i={},o=a.createContext(i);function r(e){const t=a.useContext(o);return a.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function s(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:r(e.components),a.createElement(o.Provider,{value:t},e.children)}}}]);