<template>
  <div class="conflict-detection-container">
    <!-- 冲突检测面板 -->
    <el-card class="conflict-panel">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">智能冲突检测</span>
          <el-button 
            @click="togglePanel" 
            type="text" 
            :icon="isPanelExpanded ? 'ArrowDown' : 'ArrowRight'"
            size="small"
          >
            {{ isPanelExpanded ? '收起' : '展开' }}
          </el-button>
        </div>
      </template>

      <div v-if="isPanelExpanded" class="panel-content">
        <!-- 日程输入表单 -->
        <el-form 
          :model="bookingForm" 
          :rules="formRules" 
          ref="bookingFormRef"
          label-width="100px"
          class="booking-form"
        >
          <el-form-item label="事件标题" prop="eventTitle">
            <el-input 
              v-model="bookingForm.eventTitle" 
              placeholder="请输入事件标题"
              clearable
            />
          </el-form-item>
          
          <el-form-item label="日期" prop="proposedDate">
            <el-date-picker
              v-model="bookingForm.proposedDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          
          <el-form-item label="开始时间" prop="startTime">
            <el-time-picker
              v-model="bookingForm.startTime"
              placeholder="选择开始时间"
              format="HH:mm"
              value-format="HH:mm"
            />
          </el-form-item>
          
          <el-form-item label="结束时间" prop="endTime">
            <el-time-picker
              v-model="bookingForm.endTime"
              placeholder="选择结束时间"
              format="HH:mm"
              value-format="HH:mm"
              :min-time="bookingForm.startTime"
            />
          </el-form-item>
          
          <el-form-item label="地点" prop="location">
            <el-input 
              v-model="bookingForm.location" 
              placeholder="请输入地点"
              clearable
            />
          </el-form-item>
          
          <el-form-item label="描述" prop="description">
            <el-input 
              v-model="bookingForm.description" 
              type="textarea"
              :rows="3"
              placeholder="请输入事件描述"
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              @click="handleCheckConflict"
              :loading="isChecking"
              icon="Search"
            >
              检测冲突
            </el-button>
            <el-button 
              @click="handleGetSuggestions"
              :loading="isGettingSuggestions"
              icon="Magic"
            >
              智能建议
            </el-button>
            <el-button @click="resetForm" icon="Refresh">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 冲突检测结果 -->
        <div v-if="conflictResult" class="result-section">
          <el-divider content-position="left">检测结果</el-divider>
          
          <div :class="['result-card', `severity-${conflictResult.severity.toLowerCase()}`]">
            <div class="result-header">
              <el-tag 
                :type="getSeverityType(conflictResult.severity)"
                size="large"
              >
                {{ getSeverityText(conflictResult.severity) }}
              </el-tag>
              <span class="conflict-count">
                冲突事件: {{ conflictResult.conflictingEvents.length }} 个
              </span>
            </div>
            
            <div class="result-message">
              {{ conflictResult.message }}
            </div>

            <!-- 冲突事件列表 -->
            <div v-if="conflictResult.conflictingEvents.length > 0" class="conflicting-events">
              <h4>冲突事件详情:</h4>
              <el-table :data="conflictResult.conflictingEvents" size="small" stripe>
                <el-table-column prop="title" label="事件标题" />
                <el-table-column prop="startTime" label="开始时间" width="120" />
                <el-table-column prop="endTime" label="结束时间" width="120" />
                <el-table-column prop="location" label="地点" />
                <el-table-column prop="eventType" label="类型" width="80" />
              </el-table>
            </div>

            <!-- 时间建议 -->
            <div v-if="conflictResult.suggestions && conflictResult.suggestions.length > 0" class="suggestions">
              <h4>建议时间段:</h4>
              <el-row :gutter="12">
                <el-col 
                  v-for="(suggestion, index) in conflictResult.suggestions" 
                  :key="index" 
                  :span="8"
                >
                  <el-card class="suggestion-card" shadow="hover">
                    <div class="suggestion-time">
                      {{ suggestion.startTime }} - {{ suggestion.endTime }}
                    </div>
                    <div class="suggestion-confidence">
                      置信度: {{ (suggestion.confidence * 100).toFixed(1) }}%
                    </div>
                    <div class="suggestion-reason">
                      {{ suggestion.reason }}
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </div>
        </div>

        <!-- 智能建议结果 -->
        <div v-if="suggestionsResult" class="suggestions-section">
          <el-divider content-position="left">智能时间建议</el-divider>
          
          <div class="suggestions-result">
            <div class="suggestions-header">
              <el-tag type="success" size="large">
                最佳时间段建议
              </el-tag>
              <span class="suggestions-date">
                日期: {{ suggestionsResult.date }}
              </span>
            </div>
            
            <div class="suggestions-message">
              {{ suggestionsResult.message }}
            </div>

            <!-- 用户偏好 -->
            <div v-if="suggestionsResult.userPreferences" class="user-preferences">
              <h4>您的偏好设置:</h4>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="工作日开始">
                  {{ suggestionsResult.userPreferences.workDayStart }}
                </el-descriptions-item>
                <el-descriptions-item label="工作日结束">
                  {{ suggestionsResult.userPreferences.workDayEnd }}
                </el-descriptions-item>
                <el-descriptions-item label="默认时长">
                  {{ suggestionsResult.userPreferences.defaultEventDuration }} 分钟
                </el-descriptions-item>
                <el-descriptions-item label="缓冲时间">
                  前{{ suggestionsResult.userPreferences.bufferTimeBeforeEvents }}分钟
                  后{{ suggestionsResult.userPreferences.bufferTimeAfterEvents }}分钟
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 最佳时间段 -->
            <div v-if="suggestionsResult.optimalSlots && suggestionsResult.optimalSlots.length > 0" class="optimal-slots">
              <h4>推荐时间段:</h4>
              <el-table :data="suggestionsResult.optimalSlots" size="small" stripe>
                <el-table-column label="时间段" width="200">
                  <template #default="{ row }">
                    {{ row.startTime }} - {{ row.endTime }}
                  </template>
                </el-table-column>
                <el-table-column prop="confidence" label="置信度" width="100">
                  <template #default="{ row }">
                    {{ (row.confidence * 100).toFixed(1) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="reason" label="推荐理由" />
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import ConflictDetector from '../utils/ConflictDetector.js';

export default {
  name: 'ConflictDetection',
  setup() {
    const conflictDetector = new ConflictDetector();
    const isPanelExpanded = ref(true);
    const isChecking = ref(false);
    const isGettingSuggestions = ref(false);
    const conflictResult = ref(null);
    const suggestionsResult = ref(null);
    const bookingFormRef = ref();

    // 表单数据
    const bookingForm = reactive({
      eventTitle: '',
      proposedDate: '',
      startTime: '',
      endTime: '',
      location: '',
      description: ''
    });

    // 表单验证规则
    const formRules = {
      eventTitle: [
        { required: true, message: '请输入事件标题', trigger: 'blur' }
      ],
      proposedDate: [
        { required: true, message: '请选择日期', trigger: 'change' }
      ],
      startTime: [
        { required: true, message: '请选择开始时间', trigger: 'change' }
      ],
      endTime: [
        { required: true, message: '请选择结束时间', trigger: 'change' }
      ]
    };

    // 组件方法
    const togglePanel = () => {
      isPanelExpanded.value = !isPanelExpanded.value;
    };

    const disabledDate = (time) => {
      return time.getTime() < Date.now() - 8.64e7; // 禁用今天之前的日期
    };

    const handleCheckConflict = async () => {
      try {
        const valid = await bookingFormRef.value.validate();
        if (!valid) return;

        isChecking.value = true;
        conflictResult.value = null;
        suggestionsResult.value = null;

        const result = await conflictDetector.checkConflict(bookingForm);
        conflictResult.value = result;
        
        ElMessage.success('冲突检测完成');
      } catch (error) {
        console.error('冲突检测失败:', error);
        ElMessage.error('冲突检测失败，请重试');
      } finally {
        isChecking.value = false;
      }
    };

    const handleGetSuggestions = async () => {
      try {
        if (!bookingForm.proposedDate) {
          ElMessage.warning('请先选择日期');
          return;
        }

        isGettingSuggestions.value = true;
        conflictResult.value = null;
        suggestionsResult.value = null;

        const params = {
          date: bookingForm.proposedDate,
          duration: 60, // 默认60分钟
          eventType: bookingForm.eventTitle ? 'MEETING' : undefined,
          location: bookingForm.location
        };

        const result = await conflictDetector.getSmartSuggestions(params);
        suggestionsResult.value = result;
        
        ElMessage.success('智能建议获取完成');
      } catch (error) {
        console.error('获取智能建议失败:', error);
        ElMessage.error('获取智能建议失败，请重试');
      } finally {
        isGettingSuggestions.value = false;
      }
    };

    const resetForm = () => {
      bookingFormRef.value.resetFields();
      conflictResult.value = null;
      suggestionsResult.value = null;
      
      // 设置默认值
      const defaultBooking = conflictDetector.createDefaultBooking();
      Object.assign(bookingForm, defaultBooking);
    };

    const getSeverityType = (severity) => {
      return conflictDetector.getSeverityColor(severity);
    };

    const getSeverityText = (severity) => {
      return conflictDetector.getSeverityText(severity);
    };

    // 初始化
    onMounted(() => {
      const defaultBooking = conflictDetector.createDefaultBooking();
      Object.assign(bookingForm, defaultBooking);
    });

    return {
      isPanelExpanded,
      isChecking,
      isGettingSuggestions,
      conflictResult,
      suggestionsResult,
      bookingForm,
      formRules,
      bookingFormRef,
      togglePanel,
      disabledDate,
      handleCheckConflict,
      handleGetSuggestions,
      resetForm,
      getSeverityType,
      getSeverityText
    };
  }
};
</script>

<style scoped>
.conflict-detection-container {
  margin-bottom: 20px;
}

.conflict-panel {
  border: 1px solid #e6f7ff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.panel-content {
  padding: 0 10px;
}

.booking-form {
  margin-bottom: 20px;
}

.result-section,
.suggestions-section {
  margin-top: 20px;
}

.result-card {
  padding: 16px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.result-card.severity-none {
  border-left: 4px solid #52c41a;
  background-color: #f6ffed;
}

.result-card.severity-minor {
  border-left: 4px solid #faad14;
  background-color: #fffbe6;
}

.result-card.severity-moderate {
  border-left: 4px solid #fa8c16;
  background-color: #fff7e6;
}

.result-card.severity-severe {
  border-left: 4px solid #ff4d4f;
  background-color: #fff2f0;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.conflict-count {
  color: #666;
  font-size: 14px;
}

.result-message {
  margin-bottom: 16px;
  color: #333;
  line-height: 1.5;
}

.conflicting-events,
.suggestions {
  margin-top: 16px;
}

.conflicting-events h4,
.suggestions h4 {
  margin-bottom: 12px;
  color: #333;
}

.suggestion-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.suggestion-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.suggestion-time {
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 4px;
}

.suggestion-confidence {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.suggestion-reason {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}

.suggestions-result {
  padding: 16px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  background-color: #f6ffed;
}

.suggestions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.suggestions-date {
  color: #666;
  font-size: 14px;
}

.suggestions-message {
  margin-bottom: 16px;
  color: #333;
  line-height: 1.5;
}

.user-preferences,
.optimal-slots {
  margin-top: 16px;
}

.user-preferences h4,
.optimal-slots h4 {
  margin-bottom: 12px;
  color: #333;
}

:deep(.el-descriptions__body) {
  background-color: transparent;
}

:deep(.el-table) {
  margin-top: 8px;
}
</style>