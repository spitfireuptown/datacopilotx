import { createApp } from 'vue';
import App from './App.vue';
import './tailwind.css';
import './scss/style.scss';
import pinia from './stores';
import router from './router/index';
// md
import VMdPreview from '@kangc/v-md-editor/lib/preview';
import '@kangc/v-md-editor/lib/style/preview.css';
import vuepressTheme from '@kangc/v-md-editor/lib/theme/vuepress.js';
import '@kangc/v-md-editor/lib/theme/style/vuepress.css';
// highlightjs
import hljs from 'highlight.js'; // 导入完整的highlight.js
import json from 'highlight.js/lib/languages/json'; // 导入JSON语言支持

// 注册JSON语言支持，确保SSE返回的JSON代码块能正确渲染
hljs.registerLanguage('json', json);

VMdPreview.use(vuepressTheme, {
  Hljs: hljs
});

const app = createApp(App);

app.use(pinia).use(router).use(VMdPreview).mount('#app');
