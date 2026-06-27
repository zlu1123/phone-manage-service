import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  // ==================== 忽略目录 ====================
  {
    ignores: [
      'node_modules/',
      'dist/',
      'public/',
      'build/',
      'bin/',
      'vite/plugins/auto-import.js',
      '*.min.js',
      '*.min.css',
    ],
  },

  // ==================== JS 基础规则 ====================
  js.configs.recommended,

  // ==================== Vue 规则 ====================
  ...pluginVue.configs['flat/essential'],

  // ==================== 全局变量 ====================
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2022,
        defineEmits: 'readonly',
        defineProps: 'readonly',
        defineExpose: 'readonly',
        withDefaults: 'readonly',
        defineStore: 'readonly',
        defineOptions: 'readonly',
      },
    },
  },

  // ==================== Vue 文件专属 ====================
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
  },

  // ==================== 自定义规则 ====================
  {
    rules: {
      // ---------- Vue 规则 ----------
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
      'vue/require-default-prop': 'off',
      'vue/no-mutating-props': 'warn',

      // ---------- 通用规则 ----------
      'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],

      // ---------- 代码风格（替代 Prettier） ----------
      semi: ['error', 'always'],
      quotes: ['warn', 'single', { avoidEscape: true }],
      'comma-dangle': ['warn', 'always-multiline'],
      'arrow-parens': ['warn', 'always'],
      'object-curly-spacing': ['warn', 'always'],
      indent: ['warn', 2, { SwitchCase: 1 }],
      'max-len': ['warn', { code: 120, ignoreStrings: true, ignoreTemplateLiterals: true, ignoreComments: true }],
      'no-trailing-spaces': 'warn',
      'eol-last': ['warn', 'always'],
    },
  },
]
