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
  favicon: 'img/opensrp-favicon.png',
  staticDirectories: ['static'],

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
        blog: false,
        theme: {
          customCss: [require.resolve('./static/css/custom.css')],
        }
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
      defaultMode: 'light',
      disableSwitch: true,
      respectPrefersColorScheme: false,
    },
      
      navbar: {
        title: 'Docs',
        logo: {
          alt: 'OpenSRP Documentation',
          src: 'img/opensrp-logo.png',
          href: '/',
        },
        items: [
          {
            to: 'https://opensrp.io/',
            label: 'Home',
            position: 'right',
            target: '_self',
          },
          {
            to: 'https://opensrp.io/about/',
            label: 'About',
            position: 'right',
            target: '_self',
          },
          {
            href: '/',
            label: 'Docs',
            position: 'right',
            className: 'nav-active'
          },
          {
            to: 'https://opensrp.io/screenshots/',
            label: 'Screenshots',
            position: 'right',
            target: '_self',
          },
          {
            to: 'https://opensrp.io/impact/',
            label: 'Impact',
            position: 'right',
            target: '_self',
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
                href: 'https://opensrp.io',
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
