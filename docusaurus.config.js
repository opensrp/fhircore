// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'opensrp-fhircore',
  tagline: 'fhir-core',
  url: 'https://org.smartregister.opensrp-fhricore.com',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/fhircore.png',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'org.smartregister', // Usually your GitHub org/user name.
  projectName: 'https://github.com/opensrp/fhircore.gite', // Usually your repo name.

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
     [
       'classic',
       ({
         docs: {
          routeBasePath:'/',
           // Please change this to your repo.
           // Remove this to remove the "edit this page" links.
           editUrl:'https://github.com/opensrp/fhircore/tree/main/docs',
         },
         blog: false
       }),
     ],
   ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'Opensrp FHIRCORE',
        logo: {
          alt: 'FHIR Core documentation',
          src: 'img/fhircore.png',

        },
        items: [
//          {
//            type: 'doc',
//            docId: 'in-app-reporting',
//            position: 'left',
//            label: 'Documentation',
//          },
          {
            href: 'https://github.com/opensrp/fhircore/tree/main',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        copyright: `Copyright © ${new Date().getFullYear()} OPENSRP FHIRCORE, Inc. Built by Ona.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
