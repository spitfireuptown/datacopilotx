/** @format */

import pluginJs from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';
import markdown from 'eslint-plugin-markdown';
import prettierPlugin from 'eslint-plugin-prettier';
import pluginVue from 'eslint-plugin-vue';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import autoImport from './.eslintrc-auto-import.json' with { type: 'json' };

/** @type {import('eslint').Linter.Config[]} */
export default [
  // 1. 配置全局变量
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.worker,
        ...globals.es2021,
        ...autoImport.globals
      }
    }
  },
  // 2. JavaScript 文件配置
  pluginJs.configs.recommended,
  {
    files: ['**/*.{mjs,cjs,js}'],
    plugins: {
      prettier: prettierPlugin
    },
    rules: {
      // 关闭 与prettier冲突的规则
      ...eslintConfigPrettier.rules
    }
  },
  // 3. typescript 配置
  ...tseslint.configs.recommended,
  {
    files: ['**/*.ts', '**/*.vue'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parser: tseslint.parser
    },
    /** @link https://typescript-eslint.io/rules/ */
    rules: {
      '@typescript-eslint/no-unused-vars': 'error', // 禁止定义未使用的变量
      '@typescript-eslint/explicit-module-boundary-types': 'off', // 允许不显式声明函数返回类型
      '@typescript-eslint/no-explicit-any': 'off', // 允许使用any类型
      '@typescript-eslint/no-unused-expressions': 'off', // 允许未使用的表达式
      '@typescript-eslint/no-async-promise-executor': 'off', // 允许使用async-promise-executor
      '@typescript-eslint/no-namespace': 'off', // 允许使用自定义 TypeScript 模块和命名空间。
      '@typescript-eslint/ban-ts-comment': 'off' // 允许使用 @ts-xxx 的注释
    }
  },

  // 4.  vue 配置
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: pluginVue.parser, // 用于解析 <template> 中的 Vue 模板
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        parser: tseslint.parser, // 用于解析 <script> 中的 TypeScript
        ecmaFeatures: {
          jsx: true,
          tsx: true
        }
      }
    },
    /** @link https://eslint.vuejs.org/rules/ */
    rules: {
      'vue/no-v-model-argument': 'off', // 关闭对 `v-model` 参数使用的规则。
      'vue/component-name-in-template-casing': ['error', 'PascalCase'], // 模板中组件名大小写
      // HTML标签闭合括号的空格
      'vue/html-closing-bracket-spacing': [
        'error',
        {
          startTag: 'never',
          endTag: 'never',
          selfClosingTag: 'always'
        }
      ],
      // 第一个属性的换行
      'vue/first-attribute-linebreak': [
        'error',
        {
          singleline: 'ignore',
          multiline: 'below' // below | ignore | off
        }
      ],
      // HTML标签闭合括号的换行
      'vue/html-closing-bracket-newline': [
        'error',
        {
          singleline: 'never',
          multiline: 'always' // never 强制闭合括号在同一行
        }
      ],
      // 禁止重复的属性名
      'vue/no-duplicate-attributes': [
        'error',
        {
          allowCoexistClass: true,
          allowCoexistStyle: true
        }
      ],
      /**
       * component 组件相关规则
       */
      'vue/script-setup-uses-vars': 'error', // <script setup> 变量检查
      'vue/no-unused-components': 'error', // 未使用组件检查
      'vue/component-definition-name-casing': ['error', 'PascalCase'], // 组件名大小写
      'vue/component-options-name-casing': ['error', 'PascalCase'], // 组件选项名大小写
      'vue/custom-event-name-casing': ['error', 'camelCase'], // 自定义事件名大小写
      'vue/multi-word-component-names': 'off', // 关闭组件名称必须是多单词的规则
      'vue/no-reserved-component-names': 'off', // 关闭对保留组件名称的检查
      'vue/attribute-hyphenation': 'off', // 对模板中的自定义组件强制执行属性命名样式
      // defineMacros 顺序
      'vue/define-macros-order': [
        'error',
        {
          order: ['defineProps', 'defineEmits']
        }
      ],
      /**
       * props 模板相关规则
       */
      'vue/prop-name-casing': 'off', // 关闭 `prop` 名称大小写规则的检查
      'vue/require-default-prop': 'off', // 关闭要求组件 `prop` 必须有默认值的规则
      'vue/require-prop-types': 'off', // 关闭要求组件 `prop` 必须有类型定义的规则
      'vue/no-mutating-props': 'error', // props 不可变性检查
      /**
       * 其它规则
       */
      'vue/no-async-in-computed-properties': 'error', // 不允许在计算属性中使用async
      'vue/no-extra-parens': ['error', 'all'], // 不允许使用多余的括号
      'vue/multiline-ternary': ['error', 'always-multiline'], // 要求多行三元运算符的换行
      'vue/operator-linebreak': 'off', // 关闭操作符换行规则的检查
      'vue/quote-props': 'off', // 关闭属性名引号使用规则的检查
      'vue/no-irregular-whitespace': 'off', // 关闭检查不规则空白字符的规则

      // 关闭 与prettier冲突的 规则
      ...eslintConfigPrettier.rules
    }
  },

  // 5. markdown配置
  ...markdown.configs.recommended,

  // 6. 通用规则
  {
    rules: {
      'no-var': 'error', // 不允许使用var定义
      'prefer-const': 'error', // 强制在可能的情况下使用 const 声明变量，而不是 let
      'no-empty': ['error', { allowEmptyCatch: true }], // 允许catch空着
      'no-alert': 'error', // 不允许使用alert
      'no-multiple-empty-lines': ['warn', { max: 1 }], // 不允许多个空行
      'no-unexpected-multiline': 'error', // 禁止空余的多行
      eqeqeq: ['error', 'smart'], // 比较的时候使用严格等于
      curly: ['error', 'all'], // 强制使用花括号的风格
      'default-case': 'off', // 要求 switch 语句中有 default 分支
      'switch-colon-spacing': ['error', { before: false, after: true }], // switch 冒号后要有空格
      'brace-style': ['error', '1tbs', { allowSingleLine: true }], // 大括号风格 ["error", "1tbs"]
      'no-dupe-keys': 'error', // 对象中不允许出现重复的键
      'no-sparse-arrays': 'error', // 禁止稀疏数组， [1,,2]
      'block-scoped-var': 'error', // 将变量声明放在合适的代码块里
      'no-self-compare': 'error', // 不允许自身比较
      'no-const-assign': 'error', // 禁止修改const声明的变量
      'no-redeclare': 'error', // 禁止重复声明变量
      'no-func-assign': 'error', // 禁止重复的函数声明
      'no-shadow': 'error' // 外部作用域中的变量不能与它所包含的作用域中的变量或参数同名
    }
  },
  // 定义忽略内容
  {
    ignores: [
      '**/node_modules/**',
      '**/dist/**',
      '**/build/**',
      'public/',
      '*.md',
      '.local',
      '.git',
      '.vscode',
      '.husky',
      'Dockerfile',
      'package-lock.json',
      'pnpm-lock.yaml'
    ]
  }
];
