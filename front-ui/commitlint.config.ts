module.exports = {
  ignores: [(commit) => commit.includes('init')],
  extends: ['@commitlint/config-conventional'],
  rules: {
    'body-leading-blank': [2, 'always'],
    'footer-leading-blank': [1, 'always'],
    'header-max-length': [2, 'always', 108],
    'subject-empty': [2, 'never'],
    'type-empty': [2, 'never'],
    'subject-case': [0],
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'perf',
        'style',
        'docs',
        'test',
        'refactor',
        'build',
        'ci', // 修改 CI 配置、脚本
        'chore', // 对脚手架配置或辅助工具和依赖的更改
        'revert',
        'wip', // 开发中
        'workflow', //工作流改进
        'types', // 类型定义文件更改
        'release'
      ]
    ]
  },
  prompt: {
    messages: {
      type: '选择你要提交的类型 :',
      scope: '选择一个提交范围（可选）:',
      customScope: '请输入自定义的提交范围 :',
      subject: '填写简短精炼的变更描述 :\n',
      body: '填写更加详细的变更描述（可选）。使用 "|" 换行 :\n',
      breaking: '列举非兼容性重大的变更（可选）。使用 "|" 换行 :\n',
      footerPrefixesSelect: '选择关联issue前缀（可选）:',
      customFooterPrefix: '输入自定义issue前缀 :',
      footer: '列举关联issue (可选) 例如: #31, #I3244 :\n',
      generatingByAI: '正在通过 AI 生成你的提交简短描述...',
      generatedSelectByAI: '选择一个 AI 生成的简短描述:',
      confirmCommit: '是否提交或修改commit ?'
    },
    // prettier-ignore
    types: [
      { value: 'feat', name: 'feat:     ✨  新增功能', emoji: ':sparkles:' },
      { value: 'fix', name: 'fix:     🐛  修复缺陷', emoji: ':bug:' },
      { value: 'docs', name: 'docs:     📝  文档更新', emoji: ':memo:' },
      { value: 'style', name: 'style:     💄  代码格式 | Changes that do not affect the meaning of the code', emoji: ':lipstick:' },
      { value: 'refactor', name: 'refactor:     ♻️  代码重构 | A code change that neither fixes a bug nor adds a feature', emoji: ':recycle:' },
      { value: 'perf', name: 'perf:     ⚡️  性能优化', emoji: ':zap:' },
      { value: 'test', name: 'test:     ✅  测试相关', emoji: ':white_check_mark:' },
      {
        value: 'build',
        name: 'build:     📦️  构建流程、依赖变更相关',
        emoji: ':package:'
      },
      { value: 'ci', name: 'ci:     🎡  持续集成', emoji: ':ferris_wheel:' },
      { value: 'revert', name: 'revert:     ⏪️  回退代码 ', emoji: ':rewind:' },
      { value: 'chore', name: 'chore:     🔨  其他修改 | Other changes that do not modify src or test files', emoji: ':hammer:' }
    ],
    useEmoji: true,
    emojiAlign: 'center',
    allowCustomScopes: true,
    allowEmptyScopes: true,
    customScopesAlign: 'bottom',
    customScopesAlias: 'custom',
    emptyScopesAlias: 'empty',
    upperCaseSubject: false,
    markBreakingChangeMode: false,
    allowBreakingChanges: ['feat', 'fix'],
    breaklineNumber: 100,
    breaklineChar: '|',
    skipQuestions: [],
    issuePrefixes: [
      { value: 'wip', name: 'wip:      将任务状态更改为进行中' },
      { value: 'finish', name: 'finish:   将任务状态更改为待完成' },
      // 如果使用 gitee 作为开发管理
      { value: 'link', name: 'link:     链接 ISSUES 进行中' },
      { value: 'closed', name: 'closed:   标记 ISSUES 已完成' }
    ],
    customIssuePrefixAlign: 'top',
    emptyIssuePrefixAlias: 'skip',
    customIssuePrefixAlias: 'custom',
    allowCustomIssuePrefix: true,
    allowEmptyIssuePrefix: true,
    confirmColorize: true,
    scopeOverrides: undefined
  }
};
