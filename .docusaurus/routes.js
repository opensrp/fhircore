import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/',
    component: ComponentCreator('/', '4f9'),
    routes: [
      {
        path: '/',
        component: ComponentCreator('/', '678'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/fhir-core-documetation',
        component: ComponentCreator('/fhir-core-documetation', '6f4'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/in-app-reporting',
        component: ComponentCreator('/in-app-reporting', '516'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/keycloak-auth-token-configuration',
        component: ComponentCreator('/keycloak-auth-token-configuration', 'b65'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/testing-resources',
        component: ComponentCreator('/testing-resources', '13e'),
        exact: true,
        sidebar: "defaultSidebar"
      }
    ]
  },
  {
    path: '*',
    component: ComponentCreator('*'),
  },
];
