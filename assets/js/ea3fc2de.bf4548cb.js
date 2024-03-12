"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[1829],{8592:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>a,contentTitle:()=>c,default:()=>d,frontMatter:()=>o,metadata:()=>t,toc:()=>h});var r=i(5893),s=i(1151);const o={sidebar_label:"Backend"},c="",t={id:"engineering/backend/readme",title:"readme",description:"Backend application setup",source:"@site/docs/engineering/backend/readme.mdx",sourceDirName:"engineering/backend",slug:"/engineering/backend/",permalink:"/engineering/backend/",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/backend/readme.mdx",tags:[],version:"current",frontMatter:{sidebar_label:"Backend"},sidebar:"defaultSidebar",previous:{title:"Bundling Configurations to Target Specific App Versions",permalink:"/engineering/android-app/system-design/configs-versioning"},next:{title:"Architecture",permalink:"/engineering/backend/architecture"}},a={},h=[{value:"Backend application setup",id:"backend-application-setup",level:2},{value:"User management",id:"user-management",level:2},{value:"FHIR API user management",id:"fhir-api-user-management",level:3},{value:"Android application",id:"android-application",level:2},{value:"FHIR API and configuration resources",id:"fhir-api-and-configuration-resources",level:2},{value:"Deploy the FHIR Gateway",id:"deploy-the-fhir-gateway",level:2},{value:"Deploy fhir-web",id:"deploy-fhir-web",level:2},{value:"Gotchas",id:"gotchas",level:2},{value:"Resources",id:"resources",level:2}];function l(e){const n={a:"a",blockquote:"blockquote",code:"code",h1:"h1",h2:"h2",h3:"h3",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,s.a)(),...e.components};return(0,r.jsxs)(r.Fragment,{children:[(0,r.jsx)(n.h1,{id:""}),"\n",(0,r.jsx)(n.h2,{id:"backend-application-setup",children:"Backend application setup"}),"\n",(0,r.jsx)(n.p,{children:"The backend requires at a minimum two pieces of software, with an optional third:"}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsx)(n.li,{children:"a FHIR Store, e.g HAPI FHIR"}),"\n",(0,r.jsxs)(n.li,{children:["the ",(0,r.jsx)(n.a,{href:"https://github.com/onaio/fhir-gateway-plugin",children:"FHIR Information Gateway"})," with ",(0,r.jsx)(n.a,{href:"https://hub.docker.com/r/onaio/fhir-gateway-plugin/tags",children:"OpenSRP plugins"})]}),"\n",(0,r.jsxs)(n.li,{children:["[Optional] the ",(0,r.jsx)(n.a,{href:"https://github.com/onaio/fhir-web",children:"fhir-web"})," admin dashboard."]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"user-management",children:"User management"}),"\n",(0,r.jsxs)(n.p,{children:["You can manage users manually via the APIs and/or user interfaces for keycloak and your FHIR API, or via the fhir-web user interface. See ",(0,r.jsx)(n.a,{href:"backend/keycloak",children:"Keycloak"})," for details."]}),"\n",(0,r.jsx)(n.h3,{id:"fhir-api-user-management",children:"FHIR API user management"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["For each ",(0,r.jsx)(n.code,{children:"Practitioner"})," resource, create a corresponding ",(0,r.jsx)(n.code,{children:"Group"})," resource with the ",(0,r.jsx)(n.code,{children:"Practitioner.id"})," referenced in the ",(0,r.jsx)(n.code,{children:"Group.member"})," attribute."]}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["This ",(0,r.jsx)(n.code,{children:"Group"})," resource links the ",(0,r.jsx)(n.code,{children:"Practitioner"})," to a ",(0,r.jsx)(n.code,{children:"CarePlan"})," resource when the ",(0,r.jsx)(n.code,{children:"Practitioner"})," is the ",(0,r.jsx)(n.code,{children:"CarePlan.subject"}),"."]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["When creating a ",(0,r.jsx)(n.code,{children:"Practitioner"})," resource, create a ",(0,r.jsx)(n.code,{children:"PractitionerRole"})," resource."]}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["This resource links the ",(0,r.jsx)(n.code,{children:"Practitioner"})," to an ",(0,r.jsx)(n.code,{children:"Organization"})," resource when the ",(0,r.jsx)(n.code,{children:"Practitioner"})," is an ",(0,r.jsx)(n.code,{children:"Organization"})," member."]}),"\n",(0,r.jsxs)(n.li,{children:["The ",(0,r.jsx)(n.code,{children:"PractitionerRole"})," resource defines the role of the ",(0,r.jsx)(n.code,{children:"Practitioner"})," in the ",(0,r.jsx)(n.code,{children:"Organization"}),", e.g. a Community Health Worker or Supervisor role."]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Assign the ",(0,r.jsx)(n.code,{children:"Practitioner"})," a ",(0,r.jsx)(n.code,{children:"CareTeam"})," by adding a ",(0,r.jsx)(n.code,{children:"Practitioner"})," reference to the ",(0,r.jsx)(n.code,{children:"CareTeam.participant.member"})," attribute."]}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["Assign the ",(0,r.jsx)(n.code,{children:"CareTeam"})," an ",(0,r.jsx)(n.code,{children:"Organization"})," by adding an ",(0,r.jsx)(n.code,{children:"Organization"})," reference to the ",(0,r.jsx)(n.code,{children:"CareTeam.managingOrganization"})," attribute."]}),"\n",(0,r.jsxs)(n.li,{children:["Add an ",(0,r.jsx)(n.code,{children:"Organization"})," reference to the ",(0,r.jsx)(n.code,{children:"CareTeam.participant.member"})," attribute of the ",(0,r.jsx)(n.code,{children:"CareTeam"})," resource for easy search."]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Assign the ",(0,r.jsx)(n.code,{children:"Organization"})," a ",(0,r.jsx)(n.code,{children:"Location"})," via the ",(0,r.jsx)(n.code,{children:"OrganizationAffiliation"})," resource."]}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["The ",(0,r.jsx)(n.code,{children:"Organization"})," is referenced on the ",(0,r.jsx)(n.code,{children:"OrganizationAffiliation.organization"})," attribute."]}),"\n",(0,r.jsxs)(n.li,{children:["The ",(0,r.jsx)(n.code,{children:"Location"})," is referenced on the ",(0,r.jsx)(n.code,{children:"OrganizationAffiliation.location"})," attribute."]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["The ",(0,r.jsx)(n.code,{children:"Location"})," child parent relationship is defined by the ",(0,r.jsx)(n.code,{children:"Location.partOf"})," attribute."]}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["The parent ",(0,r.jsx)(n.code,{children:"Location"})," is referenced on the child's ",(0,r.jsx)(n.code,{children:"Location.partOf"})," attribute."]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"android-application",children:"Android application"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Update ",(0,r.jsx)(n.code,{children:"local.properties"})," file"]}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["Update ",(0,r.jsx)(n.code,{children:"FHIR_BASE_URL"})," value to the ",(0,r.jsx)(n.code,{children:"url"})," of the FHIR Gateway Host"]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"Data Filtering - configure sync strategy"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["Update the ",(0,r.jsx)(n.code,{children:"application_configuration.json"})," with the sync strategy for the deployment, e.g. for sync by Location:"]}),"\n"]}),"\n",(0,r.jsx)(n.pre,{children:(0,r.jsx)(n.code,{className:"language-json",children:'"syncStrategy": ["Location"]\n'})}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.blockquote,{children:["\n",(0,r.jsxs)(n.p,{children:[(0,r.jsx)(n.strong,{children:"Note:"})," Currently the configuration accepts an array but a subsequent update will enforce a single value. See ",(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/main/android/quest/src/main/assets/configs/app/application_config.json",children:"application_config.json"})]}),"\n"]}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"Composition JSON"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsx)(n.li,{children:"Update the identifier to the value of the application id"}),"\n"]}),"\n",(0,r.jsx)(n.pre,{children:(0,r.jsx)(n.code,{className:"language-json",children:'"identifier": {\n    "use": "official",\n    "value": "<app id>"\n}\n'})}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.blockquote,{children:["\n",(0,r.jsxs)(n.p,{children:[(0,r.jsx)(n.strong,{children:"Note:"})," ",(0,r.jsx)(n.code,{children:"identifier.value"})," above should correspond to ",(0,r.jsx)(n.code,{children:"fhir_core_app_id"})," mentioned in the user management Keycloak section below."]}),"\n"]}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["Update the ",(0,r.jsx)(n.code,{children:"sync_config.json"})," to remove all the non-patient data resources. These should be referenced from the Composition resource so they can be exempted from the Data filter. See ",(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/blob/b7c24616d4224bd8d16c53b0c2a4f14a1075ce7c/android/quest/src/main/assets/configs/app/sync_config.json",children:"sync_config.json"})]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"fhir-api-and-configuration-resources",children:"FHIR API and configuration resources"}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"Deploy the FHIR Store, e.g HAPI"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["The steps here depend on what FHIR Store your are using. To deploy the HAPI FHIR Server using JPA, follow ",(0,r.jsx)(n.a,{href:"https://github.com/hapifhir/hapi-fhir-jpaserver-starter",children:"these"})," steps."]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:[(0,r.jsx)(n.code,{children:"POST"})," the binary resources referenced in the ",(0,r.jsx)(n.code,{children:"composition_config.json"})]}),"\n"]}),"\n"]}),"\n",(0,r.jsxs)(n.blockquote,{children:["\n",(0,r.jsxs)(n.p,{children:[(0,r.jsx)(n.strong,{children:"Note:"}),"  As described in the ",(0,r.jsx)(n.a,{href:"backend/info-gateway",children:"FHIR Gateway"})," section, the server should be in an internal network behind a DMZ and therefore not require authentication, which will be handled by the FHIR Information Gateway."]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"deploy-the-fhir-gateway",children:"Deploy the FHIR Gateway"}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["Link to the ",(0,r.jsx)(n.a,{href:"https://hub.docker.com/r/onaio/fhir-gateway-plugin/tags",children:"Docker image"})]}),"\n",(0,r.jsxs)(n.li,{children:["The main documentation for deploying can be found in the ",(0,r.jsx)(n.a,{href:"https://github.com/google/fhir-gateway/blob/main/README.md",children:"Github READ ME"}),"For configuration parameters, check out Read Me file for setting environment variables."]}),"\n",(0,r.jsx)(n.li,{children:"For configuration parameters, check out Read Me file for setting environment variables."}),"\n",(0,r.jsxs)(n.li,{children:["OpenSRP nuances: Provide/export  the System variable ",(0,r.jsx)(n.code,{children:"ALLOWED_QUERIES_FILE"})," with value ",(0,r.jsx)(n.code,{children:'"resources/hapi_page_url_allowed_queries.json"'}),(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhir-gateway/blob/main/resources/hapi_page_url_allowed_queries.json",children:"HAPI Page URL Allowed Queries"})]}),"\n",(0,r.jsxs)(n.li,{children:["For each deployment the configuration entries for resources here should match the specific  ",(0,r.jsx)(n.code,{children:"Composition"})," resource ID and ",(0,r.jsx)(n.code,{children:"Binary"})," resources IDs"]}),"\n",(0,r.jsxs)(n.li,{children:["Refer to the ",(0,r.jsx)(n.a,{href:"https://docs.google.com/document/d/1dVFwI3B6AR-J3HTgLdbM-AJuoxMorrOqe2z7y_t_B2Y/edit?usp=sharing",children:"FHIR Info Gateway Plugin Deployment Documentation"})]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"deploy-fhir-web",children:"Deploy fhir-web"}),"\n",(0,r.jsxs)(n.ol,{children:["\n",(0,r.jsxs)(n.li,{children:["The OpenSRP 2.0 web portal deployment docs can be found ",(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/blob/master/docs/fhir-web-docker-deployment.md",children:"here"})]}),"\n",(0,r.jsx)(n.li,{children:"This platform doesn\u2019t yet target the Gateway server. We are working to build plugins for it to use."}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"gotchas",children:"Gotchas"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Keycloak redirect - You need to disable ",(0,r.jsx)(n.a,{href:"https://github.com/opensrp/hapi-fhir-keycloak",children:"keycloak authentication"})," in HAPI FHIR"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsx)(n.p,{children:"Binary resource base64 encoding - You need to make sure that you properly set the Binary resource for application configuration"}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Keycloak/Role configuration -  Roles for all the different resources - including ",(0,r.jsx)(n.code,{children:"PUT"}),", ",(0,r.jsx)(n.code,{children:"POST"}),", ",(0,r.jsx)(n.code,{children:"GET"})," for Binary should exist, Client Mapper for the ",(0,r.jsx)(n.code,{children:"fhir_core_app_id"})," and corresponding user attribute should not be missing"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["The ",(0,r.jsx)(n.code,{children:"TOKEN_ISSUER"})," specified in your backend deployment should be the same Realm used by the application to fetch an access token for authentication and authorization."]}),"\n"]}),"\n"]}),"\n",(0,r.jsx)(n.pre,{children:(0,r.jsx)(n.code,{children:"env:\n  - name: TOKEN_ISSUER\n    value: https://<yourkeycloak>.smartregister.org/auth/realms/FHIR_Android\n"})}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["Remove Resource entries from the ",(0,r.jsx)(n.code,{children:"sync_confguration.json"})," file that should not be part of the normal data sync but rather part of the Composition file e.g. Questionnaire"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["When testing the set up ",(0,r.jsx)(n.strong,{children:"DO NOT"})," use debug app ids e.g. ",(0,r.jsx)(n.code,{children:"app/debug"}),". The Gateway\u2019s implementation is tightly coupled with the server hosted application resources"]}),"\n"]}),"\n",(0,r.jsxs)(n.li,{children:["\n",(0,r.jsxs)(n.p,{children:["In the HAPI FHIR application.yaml disable validations by setting to ",(0,r.jsx)(n.code,{children:"false*"}),". This is however not highly recommended."]}),"\n"]}),"\n"]}),"\n",(0,r.jsx)(n.h2,{id:"resources",children:"Resources"}),"\n",(0,r.jsxs)(n.ul,{children:["\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhir-gateway",children:"FHIR Gateway"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/discussions/1603",children:"Permission Checker Spec"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/discussions/1604",children:"Data Access Filter Spec"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/fhircore/discussions/1612",children:"Data Requesting Spec"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://hub.docker.com/r/opensrp/fhir-gateway/tags",children:"FHIR Gateway Tags"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/blob/master/docs/fhir-web-docker-deployment.md",children:"FHIR Web Docker Deployment"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/1094",children:"OpenSRP Web Issue 1094"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/1095",children:"OpenSRP Web Issue 1095"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/553",children:"OpenSRP Web Issue 553"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/842",children:"OpenSRP Web Issue 842"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/552",children:"OpenSRP Web Issue 552"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/665",children:"OpenSRP Web Issue 665"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/1080",children:"OpenSRP Web Issue 1080"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/663",children:"OpenSRP Web Issue 663"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://github.com/opensrp/web/issues/1079",children:"OpenSRP Web Issue 1079"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://docs.google.com/document/d/1MEw41Rtfdmos9gqqDamQ31_Y58E8Thgo_8i9UXD8ET4",children:"OpenSRP V2 RBAC ROLES"})}),"\n",(0,r.jsx)(n.li,{children:(0,r.jsx)(n.a,{href:"https://docs.google.com/document/d/1OeznAQsZe4p2NDiHhpfNKWB2y-qVhgEva5k_GeHTiKc/edit?usp=sharing",children:"How to Migrate to the Gateway server for sync"})}),"\n"]})]})}function d(e={}){const{wrapper:n}={...(0,s.a)(),...e.components};return n?(0,r.jsx)(n,{...e,children:(0,r.jsx)(l,{...e})}):l(e)}},1151:(e,n,i)=>{i.d(n,{Z:()=>t,a:()=>c});var r=i(7294);const s={},o=r.createContext(s);function c(e){const n=r.useContext(o);return r.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function t(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(s):e.components||s:c(e.components),r.createElement(o.Provider,{value:n},e.children)}}}]);