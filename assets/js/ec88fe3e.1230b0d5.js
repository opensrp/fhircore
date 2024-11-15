"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[4428],{4469:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>c,contentTitle:()=>t,default:()=>h,frontMatter:()=>l,metadata:()=>o,toc:()=>a});var s=i(4848),r=i(8453);const l={},t="Infrastructure setup",o={id:"engineering/backend/infrastructure-setup",title:"Infrastructure setup",description:"This page provides recommendations when setting up a production deployment.",source:"@site/docs/engineering/backend/infrastructure-setup.mdx",sourceDirName:"engineering/backend",slug:"/engineering/backend/infrastructure-setup",permalink:"/engineering/backend/infrastructure-setup",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/backend/infrastructure-setup.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Sync Strategies",permalink:"/engineering/backend/info-gateway/sync-strategies"},next:{title:"DHIS2",permalink:"/engineering/integrations/dhis2"}},c={},a=[{value:"Environments for building and deploying",id:"environments-for-building-and-deploying",level:2},{value:"Hardware requirements",id:"hardware-requirements",level:2},{value:"Server specifications",id:"server-specifications",level:3},{value:"Operating system",id:"operating-system",level:4},{value:"11 Virtual Machines",id:"11-virtual-machines",level:4},{value:"Network configurations",id:"network-configurations",level:3},{value:"Services/applications to be set up",id:"servicesapplications-to-be-set-up",level:3},{value:"Keycloak Oauth2 clients",id:"keycloak-oauth2-clients",level:2},{value:"Android client",id:"android-client",level:3},{value:"FHIR Web client",id:"fhir-web-client",level:3},{value:"Data pipelines/Analytics client",id:"data-pipelinesanalytics-client",level:3}];function d(e){const n={a:"a",admonition:"admonition",br:"br",code:"code",em:"em",h1:"h1",h2:"h2",h3:"h3",h4:"h4",header:"header",li:"li",ol:"ol",p:"p",strong:"strong",ul:"ul",...(0,r.R)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsx)(n.header,{children:(0,s.jsx)(n.h1,{id:"infrastructure-setup",children:"Infrastructure setup"})}),"\n",(0,s.jsx)(n.p,{children:"This page provides recommendations when setting up a production deployment."}),"\n",(0,s.jsx)(n.h2,{id:"environments-for-building-and-deploying",children:"Environments for building and deploying"}),"\n",(0,s.jsx)(n.p,{children:"We recommend setting up independent environments for"}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsx)(n.li,{children:"Staging: for testing and validting in progress changes"}),"\n",(0,s.jsx)(n.li,{children:"Preview: for demoing and training on stable releases"}),"\n",(0,s.jsx)(n.li,{children:"Production: for serving live traffic"}),"\n"]}),"\n",(0,s.jsx)(n.h2,{id:"hardware-requirements",children:"Hardware requirements"}),"\n",(0,s.jsx)(n.h3,{id:"server-specifications",children:"Server specifications"}),"\n",(0,s.jsx)(n.h4,{id:"operating-system",children:"Operating system"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Ubuntu (latest LTS version)"}),"\n"]}),"\n",(0,s.jsx)(n.h4,{id:"11-virtual-machines",children:"11 Virtual Machines"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["1 Analytics node (DBT, Airbyte)","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"4 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"8 GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"500 GB HD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["1 Load Balancer node","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"4 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"4 GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"100 GB HD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["3 Primary nodes","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"4 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"8 GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"500 GB HD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["4 Worker Nodes","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"8 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"32 GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"500 GB HD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["1 NFS server for backups","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"4 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"8GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"1TB HD"}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["1 Database server for PostgreSQL and Redis","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"4 CPU cores"}),"\n",(0,s.jsx)(n.li,{children:"8GB RAM"}),"\n",(0,s.jsx)(n.li,{children:"1TB HD"}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.p,{children:"We recommend 5 TB of external storage attached to the NFS server to hold PostgreSQL data backups."}),"\n",(0,s.jsx)(n.p,{children:"In addition to on-site backups, we recommend that partners provide additional off-site backups of operational databases for worst-case primary data center failure."}),"\n",(0,s.jsx)(n.h3,{id:"network-configurations",children:"Network configurations"}),"\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"1 public IP address must be provisioned for the load balancer node."}),"\n",(0,s.jsxs)(n.li,{children:["Stable high-speed internet connection","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Minimum upstream 20 mb/s, downstream 20 mb/s."}),"\n",(0,s.jsx)(n.li,{children:"All servers must have an internet connection during the setup phase."}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.li,{children:"Servers must be able to connect with the official distribution repositories."}),"\n",(0,s.jsxs)(n.li,{children:["Domains","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"All web-facing services must have a distinct DNS address. (More details on the services below.)"}),"\n",(0,s.jsx)(n.li,{children:"All domains used must point to the load balancer node's public IP."}),"\n",(0,s.jsxs)(n.li,{children:["We suggest using ",(0,s.jsx)(n.a,{href:"https://letsencrypt.org/",children:"Let's Encrypt"})," to issue certificates unless specified otherwise.","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"If Let's Encrypt is not used, certificates and private keys of the CSR (Certificate Signing Request) must be provided and manually added to the cluster."}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Access to the servers","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["SSH access","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Whitelisting a bastion server to access the load balancer node."}),"\n",(0,s.jsx)(n.li,{children:"The load balancer node should be able to access all the servers mentioned above."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Firewall  configurations (You can ignore the below if the servers will be provided without port restrictions)","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Kubernetes configuration ports have to be open on the following servers.","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsxs)(n.li,{children:["Cluster nodes (Both primary and worker nodes)","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Allow incoming/outgoing traffic between cluster nodes on all ports."}),"\n",(0,s.jsx)(n.li,{children:"Allow incoming traffic on port 22 from the load balancer node."}),"\n",(0,s.jsx)(n.li,{children:"Allow incoming/outgoing traffic on ports 2049 and 111 from/to the NFS server."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Load balancer Node","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Allow incoming/outgoing on ports 80 and 443 from/to external traffic (internet)."}),"\n",(0,s.jsx)(n.li,{children:"Allow incoming traffic on port 22 from Ona\u2019s bastion host."}),"\n",(0,s.jsx)(n.li,{children:"Allow outgoing traffic from port 22 to cluster nodes and database and NFS server."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["Database Server","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Allow incoming traffic on port 22 from the load balancer node."}),"\n",(0,s.jsx)(n.li,{children:"Allow incoming/outgoing traffic on port 5432(postgres) and 6379(redis) to/from cluster nodes."}),"\n"]}),"\n"]}),"\n",(0,s.jsxs)(n.li,{children:["NFS Server","\n",(0,s.jsxs)(n.ul,{children:["\n",(0,s.jsx)(n.li,{children:"Allow incoming traffic on port 22 from the load balancer node."}),"\n",(0,s.jsx)(n.li,{children:"Allow incoming/outgoing traffic on port 2049 and 111 to/from cluster nodes."}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.h3,{id:"servicesapplications-to-be-set-up",children:"Services/applications to be set up"}),"\n",(0,s.jsx)(n.p,{children:"We recommend hosting the following services on the above-defined cluster:"}),"\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsx)(n.li,{children:"HAPI FHIR Server - Transactional FHIR health data store"}),"\n",(0,s.jsx)(n.li,{children:"FHIR Info Gateway - Route authorization manager"}),"\n",(0,s.jsx)(n.li,{children:"Keycloak - User identity and authentication manager"}),"\n",(0,s.jsx)(n.li,{children:"FHIR Web - Admin Dashboard"}),"\n",(0,s.jsx)(n.li,{children:"Superset - Visualization and dashboard platform"}),"\n",(0,s.jsx)(n.li,{children:"Airbyte - Data transfer pipeline manager"}),"\n",(0,s.jsx)(n.li,{children:"DBT - Analytics data query manager"}),"\n",(0,s.jsx)(n.li,{children:"Data warehouse - Analytics data warehouse in Postgres"}),"\n",(0,s.jsxs)(n.li,{children:["Application monitoring - must be on different infrastructure","\n",(0,s.jsxs)(n.ol,{children:["\n",(0,s.jsx)(n.li,{children:"Sentry"}),"\n",(0,s.jsx)(n.li,{children:"Prometheus"}),"\n",(0,s.jsx)(n.li,{children:"Grafana"}),"\n",(0,s.jsx)(n.li,{children:"Graylog"}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,s.jsx)(n.h2,{id:"keycloak-oauth2-clients",children:"Keycloak Oauth2 clients"}),"\n",(0,s.jsxs)(n.p,{children:["We use ",(0,s.jsx)(n.a,{href:"https://www.keycloak.org/",children:"Keycloak"})," as our IAM server that stores users, groups, and the access roles of those groups. Before starting the set up of the Keycloak Oauth clients ensure the ",(0,s.jsx)(n.code,{children:"Service Account"})," Role is ",(0,s.jsx)(n.strong,{children:"disabled"}),".",(0,s.jsx)(n.br,{}),"\n",(0,s.jsx)(n.em,{children:"Separate"})," OAuth clients should be configured for the ETL Pipes/Analytics and the FHIR Web systems."]}),"\n",(0,s.jsx)(n.h3,{id:"android-client",children:"Android client"}),"\n",(0,s.jsxs)(n.p,{children:["Enable ",(0,s.jsx)(n.strong,{children:"Direct Access Grant only"})," - This client should be configured as a ",(0,s.jsx)(n.code,{children:"Public"})," client. To fetch a token you will not need the client secret.\nThis will use the ",(0,s.jsx)(n.code,{children:"Resource Credentials/Password"})," Grant type."]}),"\n",(0,s.jsx)(n.admonition,{type:"danger",children:(0,s.jsxs)(n.p,{children:["Do not store any sensitive data like ",(0,s.jsx)(n.em,{children:"password credentials"})," or ",(0,s.jsx)(n.em,{children:"secrets"})," in your production APK e.g. in the ",(0,s.jsx)(n.code,{children:"local.properties"})," file."]})}),"\n",(0,s.jsx)(n.h3,{id:"fhir-web-client",children:"FHIR Web client"}),"\n",(0,s.jsxs)(n.p,{children:["Enable ",(0,s.jsx)(n.strong,{children:"Client Authentication"})," and enable ",(0,s.jsx)(n.strong,{children:"Standard flow"}),". ",(0,s.jsx)(n.em,{children:"Implicit flow should only be used for local dev testing - it can be configured for stage and maybe preview but NOT production."}),".\nThis will use the ",(0,s.jsx)(n.code,{children:"Authorization Code"})," Grant type"]}),"\n",(0,s.jsx)(n.h3,{id:"data-pipelinesanalytics-client",children:"Data pipelines/Analytics client"}),"\n",(0,s.jsxs)(n.p,{children:["Enable ",(0,s.jsx)(n.strong,{children:"Client Authentication"})," and enable ",(0,s.jsx)(n.strong,{children:"Service Account Roles"}),".\nThis will use the ",(0,s.jsx)(n.code,{children:"Client Credentials"})," Grant type."]})]})}function h(e={}){const{wrapper:n}={...(0,r.R)(),...e.components};return n?(0,s.jsx)(n,{...e,children:(0,s.jsx)(d,{...e})}):d(e)}},8453:(e,n,i)=>{i.d(n,{R:()=>t,x:()=>o});var s=i(6540);const r={},l=s.createContext(r);function t(e){const n=s.useContext(l);return s.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function o(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:t(e.components),s.createElement(l.Provider,{value:n},e.children)}}}]);