package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.CalendarEventMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.UserMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightBookingService {

	private final UserMapper userMapper;
    private final CalendarEventMapper calendarEventMapper;

    public FlightBookingService(UserMapper userMapper, CalendarEventMapper calendarEventMapper) {
        this.userMapper = userMapper;
        this.calendarEventMapper = calendarEventMapper;
    }


	// 根据事件ID查找日历事件
	public CalendarEvent findCalendarEvent(Long eventId, String username) {
		CalendarEvent event = calendarEventMapper.findById(eventId);
		if (event == null) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		User user = userMapper.findById(event.getUserId());
		if (user == null || !user.getUsername().equalsIgnoreCase(username)) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		return event;
	}

	// 根据事件ID查找日历事件
	public CalendarEvent findCalendarEventById(Long eventId) {
		CalendarEvent event = calendarEventMapper.findById(eventId);
		if (event == null) {
			throw new IllegalArgumentException("Calendar event not found");
		}
		return event;
	}

	// 根据事件ID和用户ID查找日历事件
	public CalendarEvent findCalendarEventByUserId(Long eventId, Long userId) {
		CalendarEvent event = calendarEventMapper.findById(eventId);
		if (event == null) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		if (!event.getUserId().equals(userId)) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		return event;
	}



	// 根据用户ID查找该用户的所有日程
	public List<BookingDetails> getBookingsByUserId(Long userId) {
		User user = userMapper.findById(userId);
		if (user == null) {
			return List.of(); // 用户不存在，返回空列表
		}

		return calendarEventMapper.findByUserId(userId).stream()
				.map(this::toBookingDetails)
				.collect(Collectors.toList());
	}


	// 根据事件ID和用户ID查询事件详情
	public BookingDetails getBookingDetailsByUserId(String eventId, Long userId) {
		var event = findCalendarEventByUserId(Long.valueOf(eventId), userId);
		return toBookingDetails(event);
	}



	// 根据用户ID更改日历事件
	public void changeBookingByUserId(String eventId, Long userId, String newDate, String location, String description) {
		CalendarEvent event = findCalendarEventByUserId(Long.valueOf(eventId), userId);

		LocalDate newLocalDate = LocalDate.parse(newDate);
		event.setStartTime(newLocalDate.atStartOfDay());
		event.setEndTime(newLocalDate.atTime(23, 59));
		event.setLocation(location);
		event.setDescription(description);
		
		// 更新状态为NOT_STARTED（未开始）
		event.setStatus(CalendarEvent.Status.NOT_STARTED);

		// 设置更新时间
		event.setUpdatedAt(LocalDateTime.now());

		calendarEventMapper.update(event);
	}

	// 根据用户ID取消日历事件
	public void cancelBookingByUserId(String eventId, Long userId) {
		CalendarEvent event = findCalendarEventByUserId(Long.valueOf(eventId), userId);
		event.setStatus(CalendarEvent.Status.CANCELLED);
		// 设置更新时间
		event.setUpdatedAt(LocalDateTime.now());
		calendarEventMapper.update(event);
	}

	// 根据用户ID删除日历事件
	public void deleteBookingByUserId(String eventId, Long userId) {
		CalendarEvent event = findCalendarEventByUserId(Long.valueOf(eventId), userId);
		calendarEventMapper.deleteById(event.getId());
	}

	private BookingDetails toBookingDetails(CalendarEvent event) {
		User user = userMapper.findById(event.getUserId());
		if (user == null) {
			user = new User();
		}

		return new BookingDetails(
				String.valueOf(event.getId()),
				user.getUsername() != null ? user.getUsername() : "Unknown User",
				event.getStartTime().toLocalDate(),
				event.getStatus().name(),
				event.getLocation() != null ? event.getLocation() : "Unknown Location",
				event.getDescription() != null ? event.getDescription() : "No Description",
				event.getEventType() != null ? event.getEventType().name() : "MEETING",
				event.getTitle() != null ? event.getTitle() : "无标题会议"
		);
	}
	

	// 创建日历事件（支持通过参数传递用户ID）
	public void createBooking(String date, String location, String description, String title, Long userId) {
		createBooking(date, location, description, title, userId, "UTC");
	}

	// 创建日历事件（支持时区参数）
	public void createBooking(String date, String location, String description, String title, Long userId, String timezone) {
		// 查找用户，如果不存在则创建
		User user = userMapper.findById(userId);
		if (user == null) {
			// 用户不存在，抛出异常
			throw new IllegalArgumentException("用户不存在，ID: " + userId);
		}

		LocalDate localDate = LocalDate.parse(date);
		CalendarEvent event = new CalendarEvent();
		event.setUserId(user.getId());
		event.setStartTime(localDate.atStartOfDay());
		event.setEndTime(localDate.atTime(23, 59));
		event.setLocation(location);
		event.setDescription(description);
		event.setStatus(CalendarEvent.Status.NOT_STARTED); // 默认状态为未开始
		event.setEventType(CalendarEvent.EventType.MEETING);
		event.setPriority(CalendarEvent.Priority.MEDIUM); // 默认优先级
		event.setAllDay(false); // 默认非全天事件
		event.setVisibility(CalendarEvent.Visibility.PRIVATE); // 默认私有可见性
		// 设置时区
		if (timezone != null && !timezone.trim().isEmpty()) {
			event.setTimezone(timezone);
		} else {
			event.setTimezone("UTC");
		}
		// 添加标题字段，使用描述或位置作为标题
		if (title == null || title.trim().isEmpty()) {
			title = "会议: " + (location != null ? location : "未指定地点");
		}
		event.setTitle(title);

		// 设置创建和更新时间
		LocalDateTime now = LocalDateTime.now();
		event.setCreatedAt(now);
		event.setUpdatedAt(now);

		calendarEventMapper.insert(event);
	}
}