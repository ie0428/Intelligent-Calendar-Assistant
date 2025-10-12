package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;

import java.util.List;

/**
 * 航班预订服务接口
 */
public interface FlightBookingService {
    
    /**
     * 根据事件ID查找日历事件
     * @param eventId 事件ID
     * @param username 用户名
     * @return 日历事件
     */
    CalendarEvent findCalendarEvent(Long eventId, String username);
    
    /**
     * 根据事件ID查找日历事件
     * @param eventId 事件ID
     * @return 日历事件
     */
    CalendarEvent findCalendarEventById(Long eventId);
    
    /**
     * 根据事件ID和用户ID查找日历事件
     * @param eventId 事件ID
     * @param userId 用户ID
     * @return 日历事件
     */
    CalendarEvent findCalendarEventByUserId(Long eventId, Long userId);
    
    /**
     * 根据用户ID查找该用户的所有日程
     * @param userId 用户ID
     * @return 预订详情列表
     */
    List<BookingDetails> getBookingsByUserId(Long userId);
    
    /**
     * 根据事件ID和用户ID查询事件详情
     * @param eventId 事件ID
     * @param userId 用户ID
     * @return 预订详情
     */
    BookingDetails getBookingDetailsByUserId(String eventId, Long userId);
    
    /**
     * 根据用户ID更改日历事件
     * @param eventId 事件ID
     * @param userId 用户ID
     * @param newDate 新日期
     * @param location 地点
     * @param description 描述
     */
    void changeBookingByUserId(String eventId, Long userId, String newDate, String location, String description);
    
    /**
     * 根据用户ID取消日历事件
     * @param eventId 事件ID
     * @param userId 用户ID
     */
    void cancelBookingByUserId(String eventId, Long userId);
    
    /**
     * 根据用户ID删除日历事件
     * @param eventId 事件ID
     * @param userId 用户ID
     */
    void deleteBookingByUserId(String eventId, Long userId);
    
    /**
     * 创建日历事件（支持通过参数传递用户ID）
     * @param date 日期
     * @param location 地点
     * @param description 描述
     * @param title 标题
     * @param userId 用户ID
     */
    void createBooking(String date, String location, String description, String title, Long userId);
    
    /**
     * 创建日历事件（支持时区参数）
     * @param date 日期
     * @param location 地点
     * @param description 描述
     * @param title 标题
     * @param userId 用户ID
     * @param timezone 时区
     */
    void createBooking(String date, String location, String description, String title, Long userId, String timezone);
}