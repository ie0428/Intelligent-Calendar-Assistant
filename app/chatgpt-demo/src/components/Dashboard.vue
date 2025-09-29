<template>
  <div class="dashboard-container">
    <!-- 仪表盘标题和筛选器 -->
    <el-card class="dashboard-header">
      <div class="header-content">
        <div class="header-title">
          <h2>时间分配分析仪表盘</h2>
          <p>可视化展示您的日程时间分配情况</p>
        </div>
        <div class="header-filters">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="handleTimeRangeChange"
          />
          <el-select v-model="viewType" placeholder="视图类型" @change="handleViewTypeChange">
            <el-option label="周视图" value="week" />
            <el-option label="月视图" value="month" />
            <el-option label="季度视图" value="quarter" />
          </el-select>
          <el-button type="primary" @click="refreshData" icon="Refresh">刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <!-- 关键指标卡片 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon total-events">
              <i class="el-icon-date"></i>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.totalEvents }}</div>
              <div class="metric-label">总事件数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon total-hours">
              <i class="el-icon-time"></i>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.totalHours }}h</div>
              <div class="metric-label">总时长</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon avg-duration">
              <i class="el-icon-timer"></i>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.avgDuration }}min</div>
              <div class="metric-label">平均时长</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon conflict-rate">
              <i class="el-icon-warning"></i>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.conflictRate }}%</div>
              <div class="metric-label">冲突率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 时间分配饼图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>时间分配比例</span>
              <el-button type="text" @click="exportTimeDistribution">导出</el-button>
            </div>
          </template>
          <div id="timeDistributionChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 每日时间分布柱状图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>每日时间分布</span>
              <el-button type="text" @click="exportDailyDistribution">导出</el-button>
            </div>
          </template>
          <div id="dailyDistributionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细分析表格 -->
    <el-row :gutter="20" class="tables-row">
      <el-col :span="24">
        <el-card class="table-card">
          <template #header>
            <div class="table-header">
              <span>详细时间分析</span>
              <el-button type="primary" @click="exportAnalysisTable" size="small">导出表格</el-button>
            </div>
          </template>
          <el-table :data="analysisData" stripe v-loading="tableLoading">
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="eventType" label="事件类型" width="120" />
            <el-table-column prop="title" label="事件标题" />
            <el-table-column prop="startTime" label="开始时间" width="100" />
            <el-table-column prop="endTime" label="结束时间" width="100" />
            <el-table-column prop="duration" label="时长(分钟)" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'danger'">
                  {{ row.status === 'CONFIRMED' ? '确认' : '取消' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="viewEventDetails(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="totalItems"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 事件详情弹窗 -->
    <el-dialog
      v-model="eventDialogVisible"
      :title="selectedEvent ? selectedEvent.title : '事件详情'"
      width="600px"
    >
      <div v-if="selectedEvent" class="event-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="事件类型">{{ selectedEvent.eventType }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ selectedEvent.date }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ selectedEvent.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ selectedEvent.endTime }}</el-descriptions-item>
          <el-descriptions-item label="时长">{{ selectedEvent.duration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedEvent.status === 'CONFIRMED' ? 'success' : 'danger'">
              {{ selectedEvent.status === 'CONFIRMED' ? '确认' : '取消' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="地点" :span="2">{{ selectedEvent.location || '无' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ selectedEvent.description || '无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="eventDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import axios from 'axios';

// 数据转换函数 - 将后端API数据转换为前端需要的格式
const transformBookingData = (bookings, timeRange) => {
  if (!bookings || !Array.isArray(bookings)) {
    return [];
  }
  
  const startDate = new Date(timeRange[0]);
  const endDate = new Date(timeRange[1]);
  
  return bookings
    .filter(booking => {
      if (!booking.date) return false;
      const bookingDate = new Date(booking.date);
      return bookingDate >= startDate && bookingDate <= endDate;
    })
    .map(booking => {
      // 计算事件时长
      const startTime = booking.from ? booking.from.split(':') : ['09', '00'];
      const endTime = booking.to ? booking.to.split(':') : ['10', '00'];
      const startMinutes = parseInt(startTime[0]) * 60 + parseInt(startTime[1]);
      const endMinutes = parseInt(endTime[0]) * 60 + parseInt(endTime[1]);
      const duration = Math.max(0, endMinutes - startMinutes);
      
      // 根据bookingClass确定事件类型
      const eventTypeMap = {
        'MEETING': '会议',
        'WORK': '工作', 
        'STUDY': '学习',
        'PERSONAL': '个人',
        'OTHER': '其他'
      };
      
      return {
        id: booking.eventId || `booking-${Date.now()}`,
        date: booking.date,
        eventType: eventTypeMap[booking.bookingClass] || booking.bookingClass || '其他',
        title: booking.title || booking.name || '未命名事件',
        startTime: booking.from || '09:00',
        endTime: booking.to || '10:00',
        duration: duration,
        status: booking.bookingStatus || 'CONFIRMED',
        location: booking.location || '未指定',
        description: booking.description || '无描述'
      };
    });
};

 // 配置axios默认设置
    axios.defaults.baseURL = 'http://localhost:8080';
// 计算指标数据
const calculateMetrics = (events) => {
  const confirmedEvents = events.filter(e => e.status === 'CONFIRMED');
  const totalHours = confirmedEvents.reduce((sum, event) => sum + event.duration, 0) / 60;
  
  return {
    totalEvents: confirmedEvents.length,
    totalHours: totalHours.toFixed(1),
    avgDuration: confirmedEvents.length > 0 
      ? Math.round(confirmedEvents.reduce((sum, event) => sum + event.duration, 0) / confirmedEvents.length)
      : 0,
    conflictRate: (events.filter(e => e.status === 'CANCELLED').length / events.length * 100).toFixed(1)
  };
};

// 生成时间分配饼图数据
const generateTimeDistributionData = (events) => {
  const confirmedEvents = events.filter(e => e.status === 'CONFIRMED');
  
  if (confirmedEvents.length === 0) {
    // 如果没有数据，返回默认数据
    return [
      { value: 12, name: '会议' },
      { value: 8, name: '工作' },
      { value: 6, name: '学习' },
      { value: 4, name: '休息' },
      { value: 3, name: '娱乐' },
      { value: 2, name: '其他' }
    ];
  }
  
  // 按事件类型分组统计时长
  const typeStats = {};
  confirmedEvents.forEach(event => {
    const type = event.eventType;
    const hours = event.duration / 60;
    
    if (!typeStats[type]) {
      typeStats[type] = 0;
    }
    typeStats[type] += hours;
  });
  
  // 转换为饼图数据格式
  return Object.entries(typeStats).map(([name, value]) => ({
    value: parseFloat(value.toFixed(1)),
    name: name
  }));
};

// 生成每日时间分布数据
const generateDailyDistributionData = (events) => {
  const confirmedEvents = events.filter(e => e.status === 'CONFIRMED');
  
  if (confirmedEvents.length === 0) {
    // 如果没有数据，返回默认数据
    return [6, 7, 8, 7, 6, 2, 1];
  }
  
  // 按星期几分组统计时长
  const dayStats = [0, 0, 0, 0, 0, 0, 0]; // 周一到周日
  
  confirmedEvents.forEach(event => {
    const date = new Date(event.date);
    const dayOfWeek = date.getDay(); // 0=周日, 1=周一, ..., 6=周六
    const hours = event.duration / 60;
    
    // 将周日(0)转换为7，周一(1)转换为1，以此类推
    const index = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
    dayStats[index] += hours;
  });
  
  return dayStats.map(hours => parseFloat(hours.toFixed(1)));
};

// 生成模拟数据
const generateMockData = () => {
  const eventTypes = ['会议', '工作', '学习', '个人', '其他'];
  const statuses = ['CONFIRMED', 'CANCELLED'];
  const mockEvents = [];
  
  // 生成过去30天的模拟数据
  for (let i = 0; i < 30; i++) {
    const date = new Date();
    date.setDate(date.getDate() - 29 + i);
    const dateStr = date.toISOString().split('T')[0];
    
    // 每天生成1-3个事件
    const eventCount = Math.floor(Math.random() * 3) + 1;
    
    for (let j = 0; j < eventCount; j++) {
      const startHour = Math.floor(Math.random() * 8) + 9; // 9-17点
      const duration = Math.floor(Math.random() * 120) + 30; // 30-150分钟
      const endHour = startHour + Math.floor(duration / 60);
      const endMinute = duration % 60;
      
      mockEvents.push({
        id: `mock-${dateStr}-${j}`,
        date: dateStr,
        eventType: eventTypes[Math.floor(Math.random() * eventTypes.length)],
        title: `模拟事件 ${i+1}-${j+1}`,
        startTime: `${startHour.toString().padStart(2, '0')}:00`,
        endTime: `${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`,
        duration: duration,
        status: statuses[Math.floor(Math.random() * statuses.length)],
        location: '模拟地点',
        description: '这是一个模拟事件用于演示'
      });
    }
  }
  
  return mockEvents;
};

export default {
  name: 'Dashboard',
  setup() {
    // 响应式数据
    const timeRange = ref([
      new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30天前
      new Date().toISOString().split('T')[0] // 今天
    ]);
    const viewType = ref('week');
    const metrics = reactive({
      totalEvents: 0,
      totalHours: 0,
      avgDuration: 0,
      conflictRate: 0
    });
    const analysisData = ref([]);
    const tableLoading = ref(false);
    const currentPage = ref(1);
    const pageSize = ref(10);
    const totalItems = ref(0);
    const eventDialogVisible = ref(false);
    const selectedEvent = ref(null);
    
    // 图表实例
    let timeDistributionChart = null;
    let dailyDistributionChart = null;

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        // 时间分配饼图
        timeDistributionChart = echarts.init(document.querySelector('#timeDistributionChart'));
        
        // 每日时间分布柱状图
        dailyDistributionChart = echarts.init(document.querySelector('#dailyDistributionChart'));
        
        updateCharts();
      });
    };

    // 更新图表数据
    const updateCharts = () => {
      if (!timeDistributionChart || !dailyDistributionChart) return;

      // 根据实际数据生成时间分配饼图数据
      const timeDistributionData = generateTimeDistributionData(analysisData.value);
      
      // 时间分配饼图配置
      const timeDistributionOption = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c}小时 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: '时间分配',
            type: 'pie',
            radius: '70%',
            data: timeDistributionData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };

      // 根据实际数据生成每日时间分布数据
      const dailyDistributionData = generateDailyDistributionData(analysisData.value);
      
      // 每日时间分布柱状图配置
      const dailyDistributionOption = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: {
          type: 'value',
          name: '小时'
        },
        series: [
          {
            name: '工作时间',
            type: 'bar',
            data: dailyDistributionData,
            itemStyle: {
              color: '#1890ff'
            }
          }
        ]
      };

      timeDistributionChart.setOption(timeDistributionOption);
      dailyDistributionChart.setOption(dailyDistributionOption);
    };

    // 处理时间范围变化
    const handleTimeRangeChange = () => {
      loadData();
    };

    // 处理视图类型变化
    const handleViewTypeChange = () => {
      loadData();
    };

    // 刷新数据
    const refreshData = () => {
      loadData();
      ElMessage.success('数据已刷新');
    };

    // 加载数据
const loadData = async () => {
  tableLoading.value = true;
  try {
    // 检查是否有认证token - 使用与App.vue相同的逻辑
    const authToken = localStorage.getItem('authToken');
    if (!authToken) {
      throw new Error('用户未登录，无法获取数据');
    }
    
    // 调用后端API获取真实数据，使用与App.vue相同的API调用方式
    const response = await axios.get('/booking/list', {
      timeout: 5000 // 设置5秒超时
    });
    
    // 使用与App.vue相同的响应处理逻辑
    if (response && response.data && Array.isArray(response.data)) {
      // 转换后端数据为前端需要的格式
      const realData = transformBookingData(response.data, timeRange.value);
      const calculatedMetrics = calculateMetrics(realData);
      
      Object.assign(metrics, calculatedMetrics);
      analysisData.value = realData;
      totalItems.value = realData.length;
      
      updateCharts();
      ElMessage.success('数据加载成功');
    } else {
      throw new Error('获取到的日程数据格式不正确');
    }
  } catch (error) {
    console.error('加载数据失败:', error);
    
    // 显示详细的错误信息
    let errorMessage = '获取数据失败';
    
    if (error.response) {
      // 服务器响应了，但状态码不是2xx
      errorMessage = `服务器错误: ${error.response.status} - ${error.response.data?.message || error.response.statusText}`;
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMessage = '无法连接到后端服务，请检查服务是否运行';
    } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      errorMessage = '请求超时，后端服务响应缓慢';
    } else if (error.message.includes('Network Error') || error.message.includes('Failed to fetch')) {
      errorMessage = '网络错误，无法连接到后端服务';
    } else if (error.message.includes('未登录')) {
      errorMessage = '用户未登录，无法获取数据';
    }
    
    console.error('详细错误信息:', {
      message: error.message,
      code: error.code,
      response: error.response,
      request: error.request
    });
    
    // 如果API调用失败，使用模拟数据作为降级方案
    const mockEvents = generateMockData();
    const calculatedMetrics = calculateMetrics(mockEvents);
    
    Object.assign(metrics, calculatedMetrics);
    analysisData.value = mockEvents;
    totalItems.value = mockEvents.length;
    
    updateCharts();
    
    ElMessage.warning(`${errorMessage}，使用模拟数据`);
  } finally {
    tableLoading.value = false;
  }
};

    // 分页处理
    const handleSizeChange = (newSize) => {
      pageSize.value = newSize;
      currentPage.value = 1;
    };

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage;
    };

    // 查看事件详情
    const viewEventDetails = (event) => {
      selectedEvent.value = event;
      eventDialogVisible.value = true;
    };

    // 导出功能（模拟）
    const exportTimeDistribution = () => {
      ElMessage.info('时间分配图表导出功能开发中');
    };

    const exportDailyDistribution = () => {
      ElMessage.info('每日分布图表导出功能开发中');
    };

    const exportAnalysisTable = () => {
      ElMessage.info('分析表格导出功能开发中');
    };

    // 生命周期
    onMounted(() => {
      loadData();
      initCharts();
      
      // 监听窗口大小变化，重新渲染图表
      window.addEventListener('resize', () => {
        if (timeDistributionChart) timeDistributionChart.resize();
        if (dailyDistributionChart) dailyDistributionChart.resize();
      });
    });

    onUnmounted(() => {
      if (timeDistributionChart) {
        timeDistributionChart.dispose();
      }
      if (dailyDistributionChart) {
        dailyDistributionChart.dispose();
      }
    });

    return {
      timeRange,
      viewType,
      metrics,
      analysisData,
      tableLoading,
      currentPage,
      pageSize,
      totalItems,
      eventDialogVisible,
      selectedEvent,
      handleTimeRangeChange,
      handleViewTypeChange,
      refreshData,
      handleSizeChange,
      handleCurrentChange,
      viewEventDetails,
      exportTimeDistribution,
      exportDailyDistribution,
      exportAnalysisTable
    };
  }
};
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.dashboard-header {
  margin-bottom: 20px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
}

.header-title p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.header-filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.metrics-row {
  margin-bottom: 20px;
}

.metric-card {
  border: none;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.metric-content {
  display: flex;
  align-items: center;
  padding: 16px 0;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.metric-icon.total-events { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.metric-icon.total-hours { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.metric-icon.avg-duration { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.metric-icon.conflict-rate { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.tables-row {
  margin-bottom: 20px;
}

.table-card {
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.event-details {
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-filters {
    width: 100%;
    justify-content: space-between;
  }
  
  .charts-row .el-col {
    margin-bottom: 20px;
  }
  
  .metric-content {
    flex-direction: column;
    text-align: center;
  }
  
  .metric-icon {
    margin-right: 0;
    margin-bottom: 12px;
  }
}
</style>