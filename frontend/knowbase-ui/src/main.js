import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import zhCn from 'element-plus/dist/locale/zh-cn.js';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import './styles/theme.css';
import './styles/app-overrides.css';
import App from './App.vue';
import router from './router';

const app = createApp(App);
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}
app.use(ElementPlus, { locale: zhCn });
app.use(router);
app.mount('#app');
