"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[1750],{9595:(e,n,i)=>{i.r(n),i.d(n,{assets:()=>l,contentTitle:()=>o,default:()=>h,frontMatter:()=>r,metadata:()=>a,toc:()=>d});var t=i(5893),s=i(1151);const r={},o="Editing",a={id:"engineering/app/configuring/editing",title:"Editing",description:"You can manage edits through",source:"@site/docs/engineering/app/configuring/editing.mdx",sourceDirName:"engineering/app/configuring",slug:"/engineering/app/configuring/editing",permalink:"/engineering/app/configuring/editing",draft:!1,unlisted:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/engineering/app/configuring/editing.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Data migration",permalink:"/engineering/app/configuring/data-migration"},next:{title:"Refresh/Invalidate cache",permalink:"/engineering/app/configuring/event-management/refresh-cache"}},l={},d=[{value:"Limiting what is editable",id:"limiting-what-is-editable",level:2},{value:"Toggle visibility for select questionnaire items",id:"toggle-visibility-for-select-questionnaire-items",level:2},{value:"Block select questionnaire items from editing",id:"block-select-questionnaire-items-from-editing",level:2}];function c(e){const n={blockquote:"blockquote",code:"code",h1:"h1",h2:"h2",li:"li",ol:"ol",p:"p",pre:"pre",ul:"ul",...(0,s.a)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(n.h1,{id:"editing",children:"Editing"}),"\n",(0,t.jsx)(n.p,{children:"You can manage edits through"}),"\n",(0,t.jsxs)(n.ol,{children:["\n",(0,t.jsx)(n.li,{children:"complex structure maps to ensure that any potential downstream effects of an edit (like changing CarePlan or Task status) are accounted for, or"}),"\n",(0,t.jsx)(n.li,{children:"limiting what is editable to only those fields(data elements) that do not have downstream affects."}),"\n"]}),"\n",(0,t.jsx)(n.p,{children:"We suggest following approach (2) above to reduce the chance of errors and the resulting inconsistent data. This approach can be implemented using the two options suggested below;"}),"\n",(0,t.jsx)(n.h2,{id:"limiting-what-is-editable",children:"Limiting what is editable"}),"\n",(0,t.jsx)(n.h2,{id:"toggle-visibility-for-select-questionnaire-items",children:"Toggle visibility for select questionnaire items"}),"\n",(0,t.jsx)(n.p,{children:"Below we describe how to limit what is editable while reusing the same FHIR resources that created the data. This assumes that you have"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:"a Questionniare that data is originally entered in and will be edited in,"}),"\n",(0,t.jsx)(n.li,{children:"a StructureMap that extracts data from the QuestionnaireResponse into other resources,"}),"\n",(0,t.jsx)(n.li,{children:"a config the specifies one interface to launch the Questionnaire for creation and another to launch it for editing."}),"\n"]}),"\n",(0,t.jsxs)(n.ol,{children:["\n",(0,t.jsxs)(n.li,{children:["Create a new hidden item in the Questionnaire to hold the ",(0,t.jsx)(n.code,{children:"is-edit-mode"})," value"]}),"\n"]}),"\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{children:'{\n    "extension": [\n        {\n            "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",\n            "valueBoolean": true\n        }\n    ],\n    "linkId": "is-edit-profile",\n    "type": "boolean"\n }\n'})}),"\n",(0,t.jsxs)(n.ol,{start:"2",children:["\n",(0,t.jsx)(n.li,{children:"Pre-populate the new item on when loading the questionnaire, to use a static rule with a boolean value of true. Do this for any button or menu-item that you want to launch the edit form."}),"\n"]}),"\n",(0,t.jsxs)(n.blockquote,{children:["\n",(0,t.jsx)(n.p,{children:"Sample rule:"}),"\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{children:'{\n   "name": "isEditProfile",\n   "condition": "true",\n   "actions": [\n       "data.put(\'isEditProfile\', true)"\n   ]\n}\n'})}),"\n"]}),"\n",(0,t.jsxs)(n.blockquote,{children:["\n",(0,t.jsx)(n.p,{children:"Sample pre-population:"}),"\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{children:'{\n   "paramType": "PREPOPULATE",\n   "linkId": "is-edit-profile",\n   "dataType": "BOOLEAN",\n   "key": "isEditProfile",\n   "value": "@{isEditProfile}"\n}\n'})}),"\n"]}),"\n",(0,t.jsxs)(n.ol,{start:"3",children:["\n",(0,t.jsxs)(n.li,{children:["Use the pre-set value of the edit mode item, ",(0,t.jsx)(n.code,{children:"isEditProfile"}),", to enable the items with downstream effects in the Questionnaire, such as date of birth and gender, using the ",(0,t.jsx)(n.code,{children:"enableWhen"})," Questionnaire item attribute. This will cause those items only to show when ",(0,t.jsx)(n.code,{children:"isEditProfile"})," is false, i.e. when creating data."]}),"\n"]}),"\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{children:'"enableWhen": [\n    {\n        "question": "is-edit-profile",\n        "operator": "exists",\n        "answerBoolean": false\n    }\n],\n"enableBehavior": "any"\n'})}),"\n",(0,t.jsxs)(n.ol,{start:"4",children:["\n",(0,t.jsx)(n.li,{children:"Launch the Questionnaire from an edit menu just as would is creating, but prepopulate the previously captured items. This approach allows to you to use the existing Questionnaire and StructureMap."}),"\n"]}),"\n",(0,t.jsx)(n.h2,{id:"block-select-questionnaire-items-from-editing",children:"Block select questionnaire items from editing"}),"\n",(0,t.jsxs)(n.h1,{id:"using-readonlylinkids-config",children:["Using ",(0,t.jsx)(n.code,{children:"readOnlyLinkIds"})," config"]}),"\n",(0,t.jsx)(n.p,{children:"Below we defines how we can limit the Edit to QuestionnaireItems in Edit Mode"}),"\n",(0,t.jsx)(n.p,{children:"For the Questionnaire with Edit Mode, Assign the list of QuestionnaireItems->link-ids to the property readOnlyLinkIds of QuestionnaireConfig : like"}),"\n",(0,t.jsxs)(n.blockquote,{children:["\n",(0,t.jsx)(n.pre,{children:(0,t.jsx)(n.code,{children:'"readOnlyLinkIds": [\n        "0740ed31-554a-4069-81d9-08f9f1697a03",\n        "6c2d4f74-84d5-49cd-81dd-a53e101a6899",\n        "5f06debf-721a-457d-8540-65cac95be9a1",\n        "e5bb0db6-82a0-4200-b421-334da8a916a4"\n      ]\n'})}),"\n"]}),"\n",(0,t.jsxs)(n.p,{children:["This list is handled with fhircore app function named ",(0,t.jsx)(n.code,{children:"prepareQuestionsForReadingOrEditing"})," to restrict the input behaviour on the provided link-ids fields when Questionnaire is rendered, this allows for visbility of the data capruted previously however user is unable to make edits."]})]})}function h(e={}){const{wrapper:n}={...(0,s.a)(),...e.components};return n?(0,t.jsx)(n,{...e,children:(0,t.jsx)(c,{...e})}):c(e)}},1151:(e,n,i)=>{i.d(n,{Z:()=>a,a:()=>o});var t=i(7294);const s={},r=t.createContext(s);function o(e){const n=t.useContext(r);return t.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(s):e.components||s:o(e.components),t.createElement(r.Provider,{value:n},e.children)}}}]);