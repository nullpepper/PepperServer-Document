import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'PepperCraft',
  description: 'PepperCraft 服务器文档',
  lang: 'zh-CN',
  base: '/',

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }],
    ['meta', { property: 'og:image', content: '/docusaurus-social-card.jpg' }],
  ],

  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'PepperCraft',

    nav: [
      { text: '开始', link: '/intro' },
      { text: '指令速查', link: '/quickref' },
      { text: '联盟与领地', link: '/towns/' },
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
      { text: '常用指令速查', link: '/quickref' },
      {
        text: '基础功能',
        collapsed: false,
        items: [
          { text: '传送系统', link: '/teleport' },
          { text: '经济系统', link: '/conomy' },
          { text: '聊天系统', link: '/chat' },
          { text: '前缀与称号', link: '/prefix' },
          { text: '休闲动作', link: '/actions' },
          { text: '图片展示', link: '/image' },
          { text: '矿车玩法', link: '/minecart' },
        ],
      },
      {
        text: '玩法进阶',
        collapsed: false,
        items: [
          { text: '世界', link: '/worlds' },
          { text: '基岩版玩家', link: '/bedrock' },
          { text: 'QQ 绑定', link: '/qqbind' },
          { text: '特殊功能', link: '/special' },
        ],
      },
      {
        text: '联盟与领地',
        collapsed: false,
        items: [
          { text: '联盟', link: '/towns/' },
          { text: '个人领地', link: '/towns/PersonalClaims' },
          { text: '联盟领地', link: '/towns/GuildClaims' },
          { text: '职位与权限', link: '/towns/Roles' },
          { text: '命令速查', link: '/towns/Commands' },
          { text: '繁荣度与活跃度', link: '/towns/Advancements' },
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
