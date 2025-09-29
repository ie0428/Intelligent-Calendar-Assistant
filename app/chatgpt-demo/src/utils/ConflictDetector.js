/**
 * 智能冲突检测工具类
 * 基于OpenAPI文档中的冲突检测接口实现
 */

class ConflictDetector {
    constructor() {
        this.baseURL = 'http://localhost:8080';
    }

    /**
     * 获取认证令牌
     */
    getToken() {
        return localStorage.getItem('authToken') || '';
    }

    /**
     * 检测日程冲突
     * @param {Object} bookingDetails - 日程详情
     * @returns {Promise<Object>} 冲突检测结果
     */
    async checkConflict(bookingDetails) {
        try {
            const response = await fetch(`${this.baseURL}/api/conflict/check`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                },
                body: JSON.stringify(bookingDetails)
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const result = await response.json();
            return result;
        } catch (error) {
            console.error('冲突检测失败:', error);
            throw error;
        }
    }

    /**
     * 获取智能时间建议
     * @param {Object} params - 查询参数
     * @returns {Promise<Object>} 智能建议结果
     */
    async getSmartSuggestions(params) {
        try {
            const queryParams = new URLSearchParams({
                date: params.date,
                duration: params.duration || 60,
                ...(params.eventType && { eventType: params.eventType }),
                ...(params.location && { location: params.location })
            });

            const response = await fetch(`${this.baseURL}/api/conflict/suggestions?${queryParams}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const result = await response.json();
            return result;
        } catch (error) {
            console.error('获取智能建议失败:', error);
            throw error;
        }
    }

    /**
     * 格式化冲突严重程度文本
     * @param {string} severity - 严重程度
     * @returns {string} 格式化文本
     */
    getSeverityText(severity) {
        const severityMap = {
            'NONE': '无冲突',
            'MINOR': '轻微冲突',
            'MODERATE': '中等冲突',
            'SEVERE': '严重冲突'
        };
        return severityMap[severity] || severity;
    }

    /**
     * 格式化冲突严重程度颜色
     * @param {string} severity - 严重程度
     * @returns {string} CSS颜色类名
     */
    getSeverityColor(severity) {
        const colorMap = {
            'NONE': 'success',
            'MINOR': 'warning',
            'MODERATE': 'warning',
            'SEVERE': 'danger'
        };
        return colorMap[severity] || 'info';
    }

    /**
     * 验证日程数据格式
     * @param {Object} bookingDetails - 日程详情
     * @returns {boolean} 是否有效
     */
    validateBookingDetails(bookingDetails) {
        const requiredFields = ['proposedDate', 'startTime', 'endTime'];
        
        for (const field of requiredFields) {
            if (!bookingDetails[field]) {
                console.error(`缺少必要字段: ${field}`);
                return false;
            }
        }

        // 验证时间格式
        const timeRegex = /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/;
        if (!timeRegex.test(bookingDetails.startTime) || !timeRegex.test(bookingDetails.endTime)) {
            console.error('时间格式不正确，应为 HH:MM 格式');
            return false;
        }

        // 验证开始时间早于结束时间
        const start = new Date(`2000-01-01T${bookingDetails.startTime}`);
        const end = new Date(`2000-01-01T${bookingDetails.endTime}`);
        if (start >= end) {
            console.error('开始时间必须早于结束时间');
            return false;
        }

        return true;
    }

    /**
     * 创建默认日程对象
     * @returns {Object} 默认日程对象
     */
    createDefaultBooking() {
        const now = new Date();
        const tomorrow = new Date(now);
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        return {
            eventTitle: '',
            proposedDate: tomorrow.toISOString().split('T')[0], // YYYY-MM-DD
            startTime: '09:00',
            endTime: '10:00',
            location: '',
            description: ''
        };
    }
}

export default ConflictDetector;