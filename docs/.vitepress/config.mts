import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'PepperCraft',
  description: 'PepperCraft 服务器文档',
  lang: 'zh-CN',
  base: '/',

  // 这两份是内部设计稿（含服务端实现、数据库与插件内部细节），
  // 不属于玩家文档，故从站点构建中排除；文件仍保留在仓库里备查。
  srcExclude: [
    'placeholder-engine-design.md',
    'placeholder-engine-phase2-proposal.md',
  ],

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }],
    ['meta', { property: 'og:image', content: '/docusaurus-social-card.jpg' }],
  ],

  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'PepperCraft',

    nav: [
      { text: '开始', link: '/intro' },
      { text: '命令速查', link: '/commands' },
      {
        text: '领地与联盟',
        items: [
          { text: '个人领地', link: '/claims' },
          { text: '联盟', link: '/union' },
        ],
      },
      {
        text: '交流',
        items: [
          {
            text: 'QQ 群',
            link: 'http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=BNDtGXRLBByex13wKAbcFHKLuNgiJZ7J&authKey=luNiluenZMRaSc37Z3NFAexAJUuNLkJBzPV90DtDookyJ3hqNGfUw7AO7u2zNAtd&noverify=0&group_code=754966689',
          },
          {
            text: '官网',
            link: 'http://web.pepper.ltd',
          },
        ],
      },
      { text: 'GitHub', link: 'https://github.com/nullpepper/PepperServer-Document' },
    ],

    sidebar: [
      { text: '开始', link: '/intro' },
      {
        text: '基础',
        collapsed: false,
        items: [
          { text: '连接与快速开始', link: '/connection' },
          { text: '基础设定与规则', link: '/rules' },
          { text: '聊天与社交', link: '/chat' },
          { text: '传送与个人传送点', link: '/teleport' },
          { text: '经济系统', link: '/economy' },
        ],
      },
      {
        text: '领地与联盟',
        collapsed: false,
        items: [
          { text: '个人领地', link: '/claims' },
          { text: '联盟', link: '/union' },
        ],
      },
      {
        text: '玩法',
        collapsed: false,
        items: [
          { text: '连锁采集', link: '/harvest' },
          { text: '矿车工作站', link: '/minecart' },
          { text: '主城弹射', link: '/eject' },
          { text: '称号系统', link: '/titles' },
          { text: '休闲动作', link: '/actions' },
        ],
      },
      {
        text: '其他',
        collapsed: false,
        items: [
          { text: '基岩版与跨版本', link: '/bedrock' },
          { text: '命令速查表', link: '/commands' },
          { text: '常见问题', link: '/faq' },
          { text: '本服没有的功能', link: '/unavailable' },
          { text: '快速参考卡', link: '/reference' },
        ],
      },
    ],

    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
          modal: {
            noResultsText: '未找到相关结果',
            resetButtonTitle: '清除查询',
            footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' },
          },
        },
      },
    },

    docFooter: {
      prev: '上一篇',
      next: '下一篇',
    },

    outline: {
      label: '本页目录',
    },

    darkModeSwitchLabel: '外观',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '回到顶部',
    langMenuLabel: '语言',
    externalLinkIcon: true,

    footer: {
      message: 'Pepper❤️Craft 服务器文档',
      copyright: 'Copyright © Pepper, All Rights Reserved.',
    },
  },
})
