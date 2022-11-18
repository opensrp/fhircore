import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/fhircore/',
    component: ComponentCreator('/fhircore/', '8e8'),
    routes: [
      {
        path: '/fhircore/',
        component: ComponentCreator('/fhircore/', 'c9c'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/fhircore/in-app-reporting',
        component: ComponentCreator('/fhircore/in-app-reporting', '09e'),
        exact: true,
        sidebar: "defaultSidebar"
      },
      {
        path: '/fhircore/testing-resources',
        component: ComponentCreator('/fhircore/testing-resources', 'd49'),
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
