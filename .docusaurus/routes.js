import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/',
    component: ComponentCreator('/', 'cf8'),
    routes: [
      {
        path: '/',
        component: ComponentCreator('/', '678'),
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
