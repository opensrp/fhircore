// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'OpenSRP Documentation',
  tagline: 'OpenSRP Documentation',
  url: 'https://opensrp.github.io',
  baseUrl: '/',
  trailingSlash:false,
  onBrokenLinks: 'ignore',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/fhircore.png',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'opensrp', // Usually your GitHub org/user name.
  projectName: 'fhircore', // Usually your repo name.

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
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath:'/',
          editUrl:
            'https://github.com/opensrp/fhircore/tree/main/docs',
        },
        blog: false
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'OpenSRP',
        logo: {
          alt: 'OpenSRP Documentation',
          src: 'img/fhircore.png',
        },
        items: [
          {
            href: 'https://fhircore.smartregister.org/dokka',
            label: 'Code Docs',
            position: 'right',
          },
          {
            href: 'https://github.com/opensrp/fhircore',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            items: [
              {
                label: 'OpenSRP',
                href: 'https://smartregister.org',
              },
              {
                label: 'Ona',
                href: 'https://ona.io',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Ona`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
