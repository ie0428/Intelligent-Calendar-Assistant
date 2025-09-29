import { createApp } from 'vue'
//import './style.css'
import App from './App.vue'
import ElementPlus from 'element-plus' //全局引入
import 'element-plus/dist/index.css'

// 全局错误处理
const app = createApp(App);

// 添加全局错误处理器
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误捕获:', err);
  console.error('错误组件实例:', instance);
  console.error('错误信息:', info);
  
  // 如果是TypeError且涉及undefined，提供更详细的错误信息
  if (err instanceof TypeError && err.message.includes('undefined')) {
    console.error('检测到undefined属性访问错误，可能的原因：');
    console.error('1. 尝试访问未初始化的响应式数据');
    console.error('2. localStorage中的数据格式损坏');
    console.error('3. 异步数据还未加载完成就访问');
    console.error('4. 组件生命周期中的时序问题');
  }
};

// 添加未处理的Promise拒绝处理
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的Promise拒绝:', event.reason);
  event.preventDefault();
});

app.use(ElementPlus)
app.mount('#app')