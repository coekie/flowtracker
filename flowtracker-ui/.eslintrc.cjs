module.exports = {
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:svelte/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    project: 'tsconfig.json',
    extraFileExtensions: ['.svelte'],
  },
  ignorePatterns: ['.eslintrc.cjs', 'vitest.config.js'],
  overrides: [
    {
      files: ['*.svelte'],
      parser: 'svelte-eslint-parser',
      // Parse the `<script>` in `.svelte` as TypeScript
      parserOptions: {
        parser: '@typescript-eslint/parser',
      },
    },
    // ...
  ],
  env: {
    browser: true,
  },
  rules: {
    // override/add rules settings here, such as:
    // 'svelte/rule-name': 'error'
    'no-unused-vars': ['error', {argsIgnorePattern: '^_'}],
    '@typescript-eslint/no-unused-vars': ['error', {argsIgnorePattern: '^_'}],

    // allow importing `screen`
    'no-redeclare': ['error', {builtinGlobals: false}],
  },
};
