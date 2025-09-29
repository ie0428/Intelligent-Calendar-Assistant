<template>
  <div id="app">
    <!-- 登录界面 -->
    <div v-if="!isLoggedIn" class="login-container">
      <el-card class="login-card">
        <template #header>
          <div class="login-header">
            <h2>智能日程管理助手</h2>
            <p>请登录以使用智能日程管理功能</p>
          </div>
        </template>
        
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="handleRegister" style="width: 100%">注册</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- 主界面 -->
    <div v-else class="main-container">
      <el-header class="app-header">
        <div class="header-content">
          <h2>智能日程管理助手</h2>
          <div class="header-actions">
            <el-button @click="showUserSessions" type="primary" size="small">
              查看所有会话
            </el-button>
            <el-button @click="showDashboard" type="success" size="small">
              数据仪表盘
            </el-button>
            <span class="welcome-text">欢迎, {{ currentUser.username }}</span>
            <el-button @click="handleLogout" type="danger" size="small">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <!-- 冲突检测组件 -->
        <ConflictDetection />
        
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="16">
            <el-card>
              <template #header>
                <div class="table-header">
                  <span>我的日程安排</span>
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <el-radio-group v-model="currentView" size="small">
                      <el-radio-button label="calendar">日历视图</el-radio-button>
                      <el-radio-button label="table">表格视图</el-radio-button>
                    </el-radio-group>
                    <el-button @click="getBookings" type="primary" size="small" icon="Refresh">刷新</el-button>
                  </div>
                </div>
              </template>
              
              <!-- 日历视图 -->
              <div v-if="currentView === 'calendar'">
                <CalendarView 
                  :events="tableData" 
                  @edit="editBooking"
                  @cancel="cancelBooking"
                  v-loading="loading"
                />
              </div>
              
              <!-- 表格视图 -->
              <div v-else>
                <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
                  <el-table-column prop="eventId" label="ID" width="80" />
                  <el-table-column prop="title" label="会议主题" />
                  <el-table-column prop="name" label="创建者" />
                  <el-table-column prop="date" label="日期" width="120" />
                  <el-table-column prop="from" label="开始时间" width="100" />
                  <el-table-column prop="to" label="结束时间" width="100" />
                  <el-table-column prop="bookingStatus" label="状态" width="100">
                    <template #default="scope">
                      <el-tag :type="scope.row.bookingStatus === 'CONFIRMED' ? 'success' : 'danger'">
                        {{ scope.row.bookingStatus === "CONFIRMED" ? "✅ 确认" : "❌ 取消" }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="bookingClass" label="类别" />
                  <el-table-column label="操作" fixed="right" width="180">
                    <template #default="scope">
                      <el-button size="small" type="primary" @click="editBooking(scope.row)">更改</el-button>
                      <el-button size="small" type="danger" @click="cancelBooking(scope.row)">取消</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card style="height: 600px;">
              <template #header>
                <div class="chat-header">
                  <span>智能对话助手</span>
                  <div style="display: flex; gap: 10px;">
                    <el-button @click="createNewConversation" type="success" size="small">
                      新建对话
                    </el-button>
                    <el-button @click="loadHistory" type="primary" size="small">
                      当前会话记录
                    </el-button>
                  </div>
                </div>
              </template>
              
              <div class="chat-container">
                <div class="chat-messages" ref="messagesContainer">
                  <el-timeline>
                    <el-timeline-item
                        v-for="(activity, index) in activities"
                        :key="index"
                        :icon="activity.icon"
                        :type="activity.type"
                        :color="activity.color"
                        :size="activity.size"
                        :hollow="activity.hollow"
                        :timestamp="activity.timestamp"
                    >
                      {{ activity.content }}
                    </el-timeline-item>
                  </el-timeline>
                </div>
                
                <div class="chat-input">
                  <el-input
                      v-model="msg"
                      :rows="2"
                      type="textarea"
                      placeholder="请输入您的问题或指令..."
                      @keydown.enter="sendMsg()"
                  />
                  <el-button @click="sendMsg()" type="primary" style="margin-top: 10px;">发送</el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </div>

    <!-- 历史对话记录弹窗 -->
    <el-dialog
        v-model="historyDialogVisible"
        :title="historyDialogTitle"
        width="70%"
        :before-close="handleHistoryDialogClose"
    >
      <div style="height: 400px; overflow: auto;">
        <el-timeline>
          <el-timeline-item
              v-for="(conversation, index) in historyConversations"
              :key="index"
              :timestamp="formatTimestamp(conversation.createdAt)"
              placement="top"
          >
            <el-card>
              <h4>用户: {{ conversation.userMessage }}</h4>
              <p>助手: {{ conversation.aiResponse }}</p>
            </el-card>
          </el-timeline-item>
          <el-timeline-item v-if="historyConversations.length === 0">
            <el-alert title="暂无对话记录" type="info" :closable="false" />
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="historyDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 用户会话列表弹窗 -->
    <el-dialog
        v-model="sessionsDialogVisible"
        title="我的所有会话"
        width="70%"
    >
      <el-table :data="userSessions" stripe>
        <el-table-column prop="summary" label="会话内容总结" width="300" />
        <el-table-column prop="conversationCount" label="对话数量" width="100" />
        <el-table-column prop="lastActivityTime" label="最后活动时间" width="180">
          <template #default="scope">
            {{ formatTimestamp(scope.row.lastActivityTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button 
                @click="loadSessionConversations(scope.row.sessionId)" 
                type="primary" 
                size="small"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="sessionsDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Dashboard弹窗 -->
    <el-dialog
        v-model="dashboardVisible"
        title="数据仪表盘"
        width="90%"
        :before-close="closeDashboard"
    >
      <Dashboard 
        :events="tableData" 
        @close="closeDashboard"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDashboard">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { MoreFilled } from "@element-plus/icons-vue";
import { ref, onMounted, nextTick, watch, onUnmounted } from "vue";
import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";
import CalendarView from "./components/CalendarView.vue";
import ConflictDetection from "./components/ConflictDetection.vue";
import Dashboard from "./components/Dashboard.vue";

export default {
  name: 'App',
  components: {
    CalendarView,
    ConflictDetection,
    Dashboard
  },
  setup() {
    // 添加错误边界处理
    const errorHandler = (error, instance, info) => {
      console.error('组件错误捕获:', error);
      console.error('错误实例:', instance);
      console.error('错误信息:', info);
      
      // 如果是TypeError，显示友好的错误提示
      if (error instanceof TypeError) {
        ElMessage.error('发生了一个错误，请刷新页面重试');
        console.error('类型错误详情:', error.message);
      }
    };

    // 数据验证工具函数
    const validateUserData = (data, source = 'unknown') => {
      if (!data) {
        console.warn(`[${source}] 用户数据为空`);
        return false;
      }
      if (typeof data !== 'object') {
        console.warn(`[${source}] 用户数据类型不正确:`, typeof data);
        return false;
      }
      if (!data.id || !data.username) {
        console.warn(`[${source}] 用户数据缺少必要字段:`, { id: !!data.id, username: !!data.username });
        return false;
      }
      return true;
    };

    const validateSessionData = (data, source = 'unknown') => {
      if (!data) {
        console.warn(`[${source}] 会话数据为空`);
        return false;
      }
      if (!Array.isArray(data)) {
        console.warn(`[${source}] 会话数据不是数组:`, typeof data);
        return false;
      }
      return true;
    };

    // 响应式数据
    const isLoggedIn = ref(false);
    const currentUser = ref({ id: 0, username: '', email: '' });
    const token = ref('');
    const activeTab = ref('login');
    
    // 登录表单
    const loginForm = ref({ username: '', password: '' });
    const loginFormRef = ref();
    const loginRules = {
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    };
    
    // 注册表单
    const registerForm = ref({ username: '', email: '', password: '' });
    const registerFormRef = ref();
    const registerRules = {
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
      ],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    };

    // 聊天相关状态
    const activities = ref([
      {
        content: "⭐欢迎使用ie智能日程助手！请问有什么可以帮您的?日程相关的问题，可以直接提问我哦~",
        timestamp: new Date().toLocaleString(),
        color: "#0bbd87",
      },
    ]);
    const msg = ref("");
    const tableData = ref([]);
    const loading = ref(false);
    let count = 2;
    let eventSource;
    const sessionId = ref("session-" + Date.now());
    
    // 视图切换状态
    const currentView = ref('calendar'); // 'calendar' 或 'table'
    
    // Dashboard相关状态
    const dashboardVisible = ref(false);
    
    // 历史记录相关
    const historyDialogVisible = ref(false);
    const historyDialogTitle = ref("历史对话记录");
    const historyConversations = ref([]);
    const sessionsDialogVisible = ref(false);
    const userSessions = ref([]);
    const messagesContainer = ref();

    // 配置axios默认设置
    axios.defaults.baseURL = 'http://localhost:8080';

    // 认证方法
    const handleLogin = async () => {
      try {
        const valid = await loginFormRef.value.validate();
        if (!valid) return;

        const response = await axios.post('/api/auth/signin', loginForm.value);
        const { token: authToken, id, username, email } = response.data;
        
        // 添加调试信息
        console.log('登录响应用户ID:', id);
        console.log('登录响应用户名:', username);
        console.log('登录响应邮箱:', email);
        
        token.value = authToken;
        currentUser.value = { id, username, email };
        isLoggedIn.value = true;
        
        // 设置axios默认认证头
        axios.defaults.headers.common['Authorization'] = `Bearer ${authToken}`;
        
        // 使用用户ID作为sessionId的基础，确保同一用户始终使用相同的sessionId
        sessionId.value = "user-" + id + "-session-" + Date.now();
        localStorage.setItem('sessionId', sessionId.value);
        
        ElMessage.success('登录成功！');
        getBookings();
      } catch (error) {
        console.error('登录失败:', error);
        ElMessage.error('登录失败，请检查用户名和密码');
      }
    };

    const handleRegister = async () => {
      try {
        const valid = await registerFormRef.value.validate();
        if (!valid) return;

        await axios.post('/api/auth/signup', registerForm.value);
        ElMessage.success('注册成功！请登录');
        activeTab.value = 'login';
        registerForm.value = { username: '', email: '', password: '' };
      } catch (error) {
        console.error('注册失败:', error);
        ElMessage.error('注册失败，请稍后重试');
      }
    };

    const handleLogout = () => {
      isLoggedIn.value = false;
      currentUser.value = { id: 0, username: '', email: '' };
      token.value = '';
      delete axios.defaults.headers.common['Authorization'];

      // 清理sessionId
      sessionId.value = "session-" + Date.now();
      localStorage.removeItem('sessionId');

      activities.value = [{
        content: "⭐欢迎使用智能日程助手！请问有什么可以帮您的?",
        timestamp: new Date().toLocaleString(),
        color: "#0bbd87",
      }];
      tableData.value = [];
      ElMessage.success('已退出登录');
    };

    // 聊天方法
    const sendMsg = () => {
      if (!msg.value.trim()) return;

      if (eventSource) {
        eventSource.close();
      }

      activities.value.push({
        content: `你: ${msg.value}`,
        timestamp: new Date().toLocaleString(),
        size: "large",
        type: "primary",
        icon: MoreFilled,
      });

      activities.value.push({
        content: "思考中...",
        timestamp: new Date().toLocaleString(),
        color: "#0bbd87",
      });

      // 添加调试信息
      console.log('发送消息时的用户ID:', currentUser.value.id);
      console.log('发送消息时的用户名:', currentUser.value.username);
      
      eventSource = new EventSource(
          `${axios.defaults.baseURL}/ai/generateStreamAsString?message=${encodeURIComponent(msg.value)}&sessionId=${sessionId.value}&userId=${currentUser.value.id}`
      );
      
      const currentCount = count;
      msg.value = "";
      
      eventSource.onmessage = (event) => {
        if (event.data === "[complete]") {
          count = count + 2;
          eventSource.close();
          getBookings();
          scrollToBottom();
          return;
        }
        activities.value[currentCount].content = activities.value[currentCount].content.replace('思考中...', '') + event.data;
      };
      
      eventSource.onopen = () => {
        activities.value[currentCount].content = "";
      };
      
      eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error);
        activities.value[currentCount].content = "抱歉，发生了错误，请重试";
        eventSource.close();
      };

      scrollToBottom();
    };

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer && messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    // 日程管理方法
    const getBookings = async () => {
      // 确保用户已登录
      if (!isLoggedIn.value || !currentUser.value || !currentUser.value.id) {
        console.warn('用户未登录或用户信息不完整，无法获取日程');
        tableData.value = [];
        return;
      }
      
      loading.value = true;
      try {
        const response = await axios.get('/booking/list');
        // 确保response.data存在且是数组
        if (response && response.data && Array.isArray(response.data)) {
          tableData.value = response.data;
        } else {
          console.warn('获取到的日程数据格式不正确:', response);
          tableData.value = [];
        }
      } catch (error) {
        console.error('获取日程失败:', error);
        tableData.value = []; // 确保tableData始终是数组
        if (error.response?.status === 401) {
          ElMessage.error('认证已过期，请重新登录');
          handleLogout();
        }
      } finally {
        loading.value = false;
      }
    };

    const editBooking = (booking) => {
      ElMessage.info(`准备编辑日程: ${booking.title || booking.name}`);
      // 这里可以打开编辑对话框
    };

    const cancelBooking = async (booking) => {
      try {
        await ElMessageBox.confirm(
            `确定要取消日程 "${booking.title || booking.name}" 吗?`,
            '确认取消',
            { type: 'warning' }
        );
        
        // 这里调用AI功能来取消日程
        msg.value = `请帮我取消日程：${booking.title || booking.name}`;
        sendMsg();
        
      } catch (error) {
        console.log('取消操作已取消');
      }
    };

    // 历史记录方法
    const loadHistory = async () => {
      try {
        // 确保sessionId存在
        if (!sessionId.value) {
          console.warn('sessionId不存在，无法加载历史记录');
          historyConversations.value = [];
          historyDialogTitle.value = "当前会话记录";
          historyDialogVisible.value = true;
          return;
        }
        
        const response = await axios.get(`/api/conversations/session/${sessionId.value}`);
        // 确保response.data存在且是数组
        if (response && response.data && Array.isArray(response.data)) {
          historyConversations.value = response.data;
        } else {
          console.warn('获取到的对话数据格式不正确:', response);
          historyConversations.value = [];
        }
        historyDialogTitle.value = "当前会话记录";
        historyDialogVisible.value = true;
      } catch (error) {
        console.error('获取历史对话记录失败:', error);
        historyConversations.value = [];
        historyDialogVisible.value = true;
      }
    };

    const showUserSessions = async () => {
      try {
        const response = await axios.get(`/api/conversations/sessions/summaries/current`);
        // 确保response.data存在且是数组
        if (response && response.data && Array.isArray(response.data)) {
          userSessions.value = response.data;
        } else {
          console.warn('获取到的会话数据格式不正确:', response);
          userSessions.value = [];
        }
        sessionsDialogVisible.value = true;
      } catch (error) {
        console.error('获取用户会话失败:', error);
        userSessions.value = [];
        ElMessage.error('获取会话列表失败');
      }
    };

    const showAllConversations = async () => {
      try {
        const response = await axios.get(`/api/conversations/user/current`);
        // 确保response.data存在且是数组
        if (response && response.data && Array.isArray(response.data)) {
          historyConversations.value = response.data;
        } else {
          console.warn('获取到的对话数据格式不正确:', response);
          historyConversations.value = [];
        }
        historyDialogTitle.value = "所有对话记录";
        historyDialogVisible.value = true;
      } catch (error) {
        console.error('获取所有对话记录失败:', error);
        historyConversations.value = [];
        historyDialogVisible.value = true;
      }
    };

    const loadSessionConversations = async (targetSessionId) => {
      try {
        // 验证targetSessionId
        if (!targetSessionId || typeof targetSessionId !== 'string') {
          console.warn('无效的会话ID:', targetSessionId);
          ElMessage.error('无效的会话ID');
          return;
        }
        
        const response = await axios.get(`/api/conversations/session/${targetSessionId}`);
        // 确保response.data存在且是数组
        if (response && response.data && Array.isArray(response.data)) {
          historyConversations.value = response.data;
        } else {
          console.warn('获取到的对话数据格式不正确:', response);
          historyConversations.value = [];
        }
        historyDialogTitle.value = `会话记录: ${targetSessionId}`;
        historyDialogVisible.value = true;
        sessionsDialogVisible.value = false;
      } catch (error) {
        console.error('获取指定会话记录失败:', error);
        ElMessage.error('获取会话记录失败');
      }
    };

    const createNewConversation = async () => {
      try {
        // 确认用户是否要开始新对话
        await ElMessageBox.confirm(
          '确定要开始新的对话吗？当前对话记录将被保存，但会开启一个新的对话会话。',
          '新建对话',
          { type: 'warning' }
        );
        
        // 调用后端API创建新对话
        const response = await axios.post('/api/conversations/new');
        const newSessionId = response.data;
        
        // 更新当前sessionId
        sessionId.value = newSessionId;
        
        // 清空当前对话记录
        activities.value = [{
          content: "⭐欢迎使用智能日程助手！请问有什么可以帮您的?",
          timestamp: new Date().toLocaleString(),
          color: "#0bbd87",
        }];
        
        // 重置消息计数
        count = 2;
        
        ElMessage.success('已创建新对话会话');
        console.log('新对话会话ID:', newSessionId);
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('创建新对话失败:', error);
          ElMessage.error('创建新对话失败');
        }
      }
    };

    const handleHistoryDialogClose = (done) => {
      historyDialogVisible.value = false;
      done();
    };

    const formatTimestamp = (timestamp) => {
      return new Date(timestamp).toLocaleString();
    };

    // Dashboard相关方法
    const showDashboard = () => {
      dashboardVisible.value = true;
    };

    const closeDashboard = () => {
      dashboardVisible.value = false;
    };

    onMounted(() => {
      let integrityCheck; // 提前声明变量
      console.log('应用启动，开始数据完整性检查...');
      
      // 初始化时确保所有响应式数据都有默认值
      if (!currentUser.value) {
        currentUser.value = { id: 0, username: '', email: '' };
      }
      
      // 检查本地存储的token
      const savedToken = localStorage.getItem('authToken');
      const savedUser = localStorage.getItem('currentUser');
      const savedSessionId = localStorage.getItem('sessionId');
      
      console.log('尝试从localStorage恢复数据:', {
        hasToken: !!savedToken,
        hasUser: !!savedUser,
        hasSessionId: !!savedSessionId
      });
      
      // 定期检查数据完整性
      integrityCheck = setInterval(() => {
        try {
          // 检查用户数据完整性
          if (isLoggedIn.value && (!currentUser.value || !currentUser.value.id)) {
            console.warn('检测到用户数据不完整，尝试重新初始化');
            handleLogout();
          }
          
          // 检查localStorage数据完整性
          const token = localStorage.getItem('authToken');
          const user = localStorage.getItem('currentUser');
          
          if (token && !user) {
            console.warn('检测到localStorage数据不一致：有token但没有用户信息');
          }
        } catch (error) {
          console.error('数据完整性检查失败:', error);
        }
      }, 30000); // 每30秒检查一次
      
      // 在组件卸载时清理定时器
      onUnmounted(() => {
        clearInterval(integrityCheck);
      });
      
      if (savedToken && savedUser) {
        try {
          token.value = savedToken;
          const parsedUser = JSON.parse(savedUser);
          
          // 验证解析后的用户对象是否有效
          if (parsedUser && parsedUser.id && parsedUser.username) {
            currentUser.value = parsedUser;
            
            // 添加调试信息 - 确保currentUser.value存在
        if (currentUser.value && currentUser.value.id) {
          console.log('从localStorage恢复的用户ID:', currentUser.value.id);
          console.log('从localStorage恢复的用户名:', currentUser.value.username);
        }
            
            isLoggedIn.value = true;
            axios.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
            
            // 如果存在保存的sessionId，则使用它，否则生成新的
            if (savedSessionId) {
              sessionId.value = savedSessionId;
              if (savedSessionId) {
          console.log('恢复之前的会话: ' + savedSessionId);
        }
            } else {
              sessionId.value = "user-" + currentUser.value.id + "-session-" + Date.now();
              localStorage.setItem('sessionId', sessionId.value);
            }
            
            getBookings();
          } else {
            console.warn('localStorage中的用户信息格式不正确，清理数据');
            localStorage.removeItem('authToken');
            localStorage.removeItem('currentUser');
            localStorage.removeItem('sessionId');
          }
        } catch (error) {
          console.error('解析localStorage用户数据失败:', error);
          // 清理损坏的数据
          localStorage.removeItem('authToken');
          localStorage.removeItem('currentUser');
          localStorage.removeItem('sessionId');
        }
      }
    });

    // 监听登录状态变化，保存到本地存储
    const unwatch = watch(isLoggedIn, (newVal) => {
      if (newVal) {
        // 确保currentUser.value存在且有有效属性
        if (currentUser.value && currentUser.value.id && currentUser.value.username) {
          localStorage.setItem('authToken', token.value);
          localStorage.setItem('currentUser', JSON.stringify(currentUser.value));
          localStorage.setItem('sessionId', sessionId.value);
          
          // 添加调试信息 - 确保currentUser.value存在
        if (currentUser.value && currentUser.value.id) {
          console.log('保存到localStorage的用户ID:', currentUser.value.id);
        }
        } else {
          console.warn('用户信息不完整，不保存到localStorage');
        }
      } else {
        localStorage.removeItem('authToken');
        localStorage.removeItem('currentUser');
        localStorage.removeItem('sessionId');
      }
    });

    // 监听sessionId变化，保存到本地存储
    const unwatchSession = watch(sessionId, (newVal) => {
      if (isLoggedIn.value && newVal && currentUser.value && currentUser.value.id) {
        localStorage.setItem('sessionId', newVal);
        // 添加调试信息
        if (newVal) {
            console.log('保存到localStorage的sessionId:', newVal);
          }
      }
    });

    onUnmounted(() => {
      unwatch();
      unwatchSession();
      if (eventSource) {
        eventSource.close();
      }
    });

    return {
      // 认证相关
      isLoggedIn,
      currentUser,
      activeTab,
      loginForm,
      loginFormRef,
      loginRules,
      registerForm,
      registerFormRef,
      registerRules,
      handleLogin,
      handleRegister,
      handleLogout,
      
      // 聊天相关
      activities,
      msg,
      tableData,
      loading,
      sendMsg,
      getBookings,
      editBooking,
      cancelBooking,
      
      // 视图切换
      currentView,
      
      // Dashboard相关
      dashboardVisible,
      showDashboard,
      closeDashboard,
      
      // 历史记录相关
      historyDialogVisible,
      historyDialogTitle,
      historyConversations,
      sessionsDialogVisible,
      userSessions,
      messagesContainer,
      loadHistory,
      showUserSessions,
      showAllConversations,
      loadSessionConversations,
      createNewConversation,
      handleHistoryDialogClose,
      formatTimestamp,
      
      // 工具
      MoreFilled,
      
      // 错误处理
      errorHandler,
      
      // 数据验证
      validateUserData,
      validateSessionData
    };
  },
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  margin: 20px;
}

.login-header {
  text-align: center;
  color: #333;
}

.login-header h2 {
  margin-bottom: 10px;
  color: #409EFF;
}

.login-tabs {
  margin-top: 20px;
}

.main-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.app-header {
  background: linear-gradient(90deg, #6477f0 0%, #dbacf3 100%);
  color: white;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome-text {
  margin: 0 15px;
  font-weight: bold;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-container {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 10px;
}

.chat-input {
  margin-top: 10px;
}

.el-timeline {
  padding: 0;
}

.el-timeline-item {
  padding: 5px 0;
}

:deep(.el-timeline-item__node) {
  background-color: #83bcf6;
}

:deep(.el-timeline-item__timestamp) {
  color: #666;
  font-size: 12px;
}
</style>
