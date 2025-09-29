<template>
  <div class="calendar-container">
    <div class="calendar-header">
      <el-button 
        @click="prevMonth" 
        class="month-nav-btn"
        icon="ElIconArrowLeft"
        circle 
      />
      <span class="current-month">{{ currentYear }}年{{ currentMonth }}月</span>
      <el-button 
        @click="nextMonth" 
        class="month-nav-btn"
        icon="ElIconArrowRight"
        circle 
      />
    </div>
    
    <div class="calendar-grid">
      <div class="weekdays">
        <div v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day" class="weekday">
          {{ day }}
        </div>
      </div>
      
      <div class="days">
        <div 
          v-for="day in calendarDays" 
          :key="day.date"
          :class="['day', { 
            'current-month': day.isCurrentMonth, 
            'today': day.isToday,
            'has-events': day.events.length > 0,
            'selected': isSameDay(day.date, selectedDate)
          }]"
          @click="selectDay(day)"
        >
          <div class="day-number">{{ day.date.getDate() }}</div>
          <div class="day-events">
            <div 
              v-for="event in day.events.slice(0, 2)" 
              :key="event.eventId"
              :class="['event-badge', getEventStatusClass(event.status)]"
              @click.stop="showEventDetails(event)"
            >
              {{ event.title || event.name }}
            </div>
            <div v-if="day.events.length > 2" class="more-events">
              +{{ day.events.length - 2 }}更多
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日程详情弹窗 -->
    <el-dialog
      v-model="eventDialogVisible"
      :title="selectedEvent ? (selectedEvent.title || selectedEvent.name) : '日程详情'"
      width="500px"
    >
      <div v-if="selectedEvent" class="event-details">
        <div class="detail-item">
          <span class="label">日期:</span>
          <span class="value">{{ selectedEvent.date }}</span>
        </div>
        <div class="detail-item">
          <span class="label">时间:</span>
          <span class="value">{{ selectedEvent.from }} - {{ selectedEvent.to }}</span>
        </div>
        <div class="detail-item">
          <span class="label">状态:</span>
          <el-tag :type="selectedEvent.bookingStatus === 'CONFIRMED' ? 'success' : 'danger'">
            {{ selectedEvent.bookingStatus === 'CONFIRMED' ? '✅ 确认' : '❌ 取消' }}
          </el-tag>
        </div>
        <div class="detail-item">
          <span class="label">类别:</span>
          <span class="value">{{ selectedEvent.bookingClass }}</span>
        </div>
        <div class="action-buttons">
          <el-button type="primary" @click="editEvent(selectedEvent)">更改</el-button>
          <el-button type="danger" @click="cancelEvent(selectedEvent)">取消</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 某天所有日程弹窗 -->
    <el-dialog
      v-model="dayEventsDialogVisible"
      :title="selectedDay ? formatDate(selectedDay.date) + ' 的日程' : '日程列表'"
      width="600px"
    >
      <div v-if="selectedDay && selectedDay.events.length > 0">
        <el-table :data="selectedDay.events" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="from" label="开始时间" width="100" />
          <el-table-column prop="to" label="结束时间" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.bookingStatus === 'CONFIRMED' ? 'success' : 'danger'">
                {{ scope.row.bookingStatus === 'CONFIRMED' ? '✅ 确认' : '❌ 取消' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button size="small" @click="showEventDetails(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else>
        <el-empty description="该日期没有日程安排" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { ElMessageBox } from 'element-plus';

export default {
  name: 'CalendarView',
  props: {
    events: {
      type: Array,
      default: () => []
    }
  },
  emits: ['edit', 'cancel'],
  setup(props, { emit }) {
    const currentDate = ref(new Date());
    const eventDialogVisible = ref(false);
    const dayEventsDialogVisible = ref(false);
    const selectedEvent = ref(null);
    const selectedDay = ref(null);
    const selectedDate = ref(null);

    const currentYear = computed(() => currentDate.value.getFullYear());
    const currentMonth = computed(() => currentDate.value.getMonth() + 1);

    const calendarDays = computed(() => {
      const year = currentDate.value.getFullYear();
      const month = currentDate.value.getMonth();
      
      // 获取当月第一天和最后一天
      const firstDay = new Date(year, month, 1);
      const lastDay = new Date(year, month + 1, 0);
      
      // 获取当月第一天是星期几（0-6，0代表周日）
      const firstDayOfWeek = firstDay.getDay();
      
      // 获取当月总天数
      const daysInMonth = lastDay.getDate();
      
      // 计算需要显示的上个月的天数
      const daysFromPrevMonth = firstDayOfWeek;
      
      // 计算需要显示的下个月的天数（总共42个格子 - 当月天数 - 上个月显示的天数）
      const totalCells = 42;
      const daysFromNextMonth = totalCells - daysInMonth - daysFromPrevMonth;
      
      const days = [];
      
      // 添加上个月的最后几天
      const prevMonthLastDay = new Date(year, month, 0).getDate();
      for (let i = daysFromPrevMonth - 1; i >= 0; i--) {
        const date = new Date(year, month - 1, prevMonthLastDay - i);
        days.push({
          date,
          isCurrentMonth: false,
          isToday: isToday(date),
          events: getEventsForDate(date)
        });
      }
      
      // 添加当月的所有天
      for (let i = 1; i <= daysInMonth; i++) {
        const date = new Date(year, month, i);
        days.push({
          date,
          isCurrentMonth: true,
          isToday: isToday(date),
          events: getEventsForDate(date)
        });
      }
      
      // 添加下个月的前几天
      for (let i = 1; i <= daysFromNextMonth; i++) {
        const date = new Date(year, month + 1, i);
        days.push({
          date,
          isCurrentMonth: false,
          isToday: isToday(date),
          events: getEventsForDate(date)
        });
      }
      
      return days;
    });

    function isToday(date) {
      const today = new Date();
      return date.getDate() === today.getDate() &&
             date.getMonth() === today.getMonth() &&
             date.getFullYear() === today.getFullYear();
    }

    function getEventsForDate(date) {
      const dateStr = formatDateForComparison(date);
      return props.events.filter(event => {
        // 后端返回的event.date是LocalDate格式（如"2024-01-15"），直接比较字符串
        const eventDateStr = event.date;
        return eventDateStr === dateStr;
      });
    }

    function formatDateForComparison(date) {
      // 使用本地时区的日期字符串进行比较，避免UTC转换问题
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    function formatDate(date) {
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    }

    function getEventStatusClass(status) {
      switch (status) {
        case 'CANCELLED':
          return 'cancelled'; // 已取消 - 红色
        case 'IN_PROGRESS':
          return 'in-progress'; // 进行中 - 黄色
        case 'COMPLETED':
          return 'completed'; // 已完成 - 绿色
        case 'NOT_STARTED':
        default:
          return 'not-started'; // 未开始 - 蓝色
      }
    }

    function prevMonth() {
      currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1);
    }

    function nextMonth() {
      currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1);
    }

    function showEventDetails(event) {
      selectedEvent.value = event;
      eventDialogVisible.value = true;
    }

    function showDayEvents(day) {
      selectedDay.value = day;
      dayEventsDialogVisible.value = true;
    }

    function selectDay(day) {
      selectedDate.value = day.date;
      showDayEvents(day);
    }

    function isSameDay(date1, date2) {
      if (!date1 || !date2) return false;
      return date1.getDate() === date2.getDate() &&
             date1.getMonth() === date2.getMonth() &&
             date1.getFullYear() === date2.getFullYear();
    }

    function editEvent(event) {
      emit('edit', event);
      eventDialogVisible.value = false;
    }

    async function cancelEvent(event) {
      try {
        await ElMessageBox.confirm(
          `确定要取消日程 "${event.name}" 吗?`,
          '确认取消',
          { type: 'warning' }
        );
        emit('cancel', event);
        eventDialogVisible.value = false;
      } catch (error) {
        console.log('取消操作已取消');
      }
    }

    return {
      currentDate,
      currentYear,
      currentMonth,
      calendarDays,
      eventDialogVisible,
      dayEventsDialogVisible,
      selectedEvent,
      selectedDay,
      selectedDate,
      prevMonth,
      nextMonth,
      showEventDetails,
      showDayEvents,
      selectDay,
      isSameDay,
      editEvent,
      cancelEvent,
      formatDate,
      getEventStatusClass
    };
  }
};
</script>

<style scoped>
.calendar-container {
  padding: 20px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.current-month {
  font-size: 24px;
  font-weight: 600;
  color: #1890ff;
  min-width: 140px;
  text-align: center;
  letter-spacing: 0.5px;
}

.month-nav-btn {
  background: linear-gradient(135deg, #e6a8d7 0%, #c9b2e8 100%);
  color: white;
  width: 40px;
  height: 40px;
  border: none;
  transition: all 0.3s ease;
}

.month-nav-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(230, 168, 215, 0.4);
  background: linear-gradient(135deg, #d496c8 0%, #b197e0 100%);
}

.month-nav-btn .el-icon {
  font-size: 16px;
}

.calendar-grid {
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
  background-color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  width: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  table-layout: fixed;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  background: linear-gradient(135deg, #3999f3 0%, #8dc7f7 100%);
  width: 100%;
}

.weekday {
  padding: 14px 0;
  text-align: center;
  font-weight: 500;
  color: white;
  font-size: 14px;
  border-right: 1px solid rgba(255, 255, 255, 0.2);
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.weekday:last-child {
  border-right: none;
}

.days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-auto-rows: minmax(120px, auto);
  width: 100%;
}

.day {
  min-height: 120px;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  padding: 10px 0;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.day:hover {
  background-color: #f0f9ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.06);
}

.day:not(.current-month) {
  background-color: #fafafa;
  color: #ccc;
  opacity: 0.6;
}

.day.today {
  background-color: #e6f7ff;
  border: none;
}

.day.today .day-number {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: white;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin: 0 auto 8px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.day.has-events {
  background-color: #f6ffed;
}

.day.has-events .day-number::after {
  content: '';
  position: absolute;
  top: 16px;
  right: 16px;
  width: 8px;
  height: 8px;
  background-color: #78c41a;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.day.selected {
  background-color: #f8f0fb;
  border: 2px solid #e6a8d7;
}

.day.selected:hover {
  background-color: #f0e0f5;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.7);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(82, 196, 26, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(82, 196, 26, 0);
  }
}

.day-number {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  text-align: center;
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-events {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
}

.event-badge {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #4ea6f8;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.2s ease;
  border: none;
}

.event-badge:hover {
  background-color: #4fa8f1;
  transform: translateX(2px);
}

.event-badge.cancelled {
  background-color: #ff4d4f; /* 红色 - 已取消 */
  text-decoration: line-through;
  opacity: 0.7;
}

.event-badge.in-progress {
  background-color: #faad14; /* 黄色 - 进行中 */
}

.event-badge.completed {
  background-color: #52c41a; /* 绿色 - 已完成 */
}

.event-badge.not-started {
  background-color: #4ea6f8; /* 蓝色 - 未开始 */
}

.more-events {
  font-size: 11px;
  color: #999;
  text-align: center;
  padding: 2px;
  font-style: italic;
}

.event-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label {
  font-weight: bold;
  min-width: 60px;
}

.value {
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>