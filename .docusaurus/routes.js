import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/fhircore/__docusaurus/debug',
    component: ComponentCreator('/fhircore/__docusaurus/debug', '955'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/config',
    component: ComponentCreator('/fhircore/__docusaurus/debug/config', 'bdb'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/content',
    component: ComponentCreator('/fhircore/__docusaurus/debug/content', 'aef'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/globalData',
    component: ComponentCreator('/fhircore/__docusaurus/debug/globalData', '3ef'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/metadata',
    component: ComponentCreator('/fhircore/__docusaurus/debug/metadata', 'c1c'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/registry',
    component: ComponentCreator('/fhircore/__docusaurus/debug/registry', 'fe9'),
    exact: true
  },
  {
    path: '/fhircore/__docusaurus/debug/routes',
    component: ComponentCreator('/fhircore/__docusaurus/debug/routes', 'cb2'),
    exact: true
  },
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
