package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.SmartSuggestionsRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.SmartSuggestionsResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.ConflictCheckRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.ConflictCheckResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.TimeSuggestion;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Configuration
public class BookingTools {

	@Autowired
	private FlightBookingService flightBookingService;

	@Autowired
	private ConflictDetectionService conflictDetectionService;
	
	// 存储当前请求的用户ID
	private static final ThreadLocal<Long> currentRequestUserId = new ThreadLocal<>();
	
	/**
	 * 设置当前请求的用户ID
	 * @param userId 用户ID
	 */
	public static void setCurrentRequestUserId(Long userId) {
		currentRequestUserId.set(userId);
		// 只在有用户ID时输出日志，减少重复日志
		if (userId != null) {
			System.out.println("BookingTools设置当前请求用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
		}
	}
	
	/**
	 * 清除当前请求的用户ID
	 */
	public static void clearCurrentRequestUserId() {
		Long userId = currentRequestUserId.get();
		currentRequestUserId.remove();
		// 只在有用户ID时输出清理日志
		if (userId != null) {
			System.out.println("BookingTools清理当前请求用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
		}
	}

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String eventId, String name, LocalDate date, String status,
								 String location, String description, String eventType, String title) {
	}

	// 基于用户ID的请求记录
	public record CancelBookingRequest(String eventId, Long userId) {
	}

	@Bean
	@Description("取消日程")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			try {
				log.info("开始取消日程，事件ID: {}", request.eventId());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService取消日程，用户ID: {}, 事件ID: {}", userId, request.eventId());
				flightBookingService.cancelBookingByUserId(request.eventId(), userId);
				return "日程取消成功";
			} catch (Exception e) {
				return "日程取消失败: " + e.getMessage();
			}
		};
	}

	public record DeleteBookingRequest(String eventId, Long userId) {
	}

	@Bean
	@Description("删除日程")
	public Function<DeleteBookingRequest, String> deleteBooking() {
		return request -> {
			try {
				log.info("开始删除日程，事件ID: {}", request.eventId());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService删除日程，用户ID: {}, 事件ID: {}", userId, request.eventId());
				flightBookingService.deleteBookingByUserId(request.eventId(), userId);
				return "日程删除成功";
			} catch (Exception e) {
				return "日程删除失败: " + e.getMessage();
			}
		};
	}

	public record FindCalendarEventRequest(String eventId, Long userId) {
		// 添加默认构造函数支持只传userId
		public FindCalendarEventRequest(Long userId) {
			this(null, userId);
		}
	}

	@Bean
	@Description("查找用户日程")
	public Function<FindCalendarEventRequest, List<BookingDetails>> findCalendarEvent() {
		return request -> {
			try {
				log.info("开始查找用户日程，事件ID: {}, 用户ID: {}", request.eventId(), request.userId());
				
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用请求参数中的userId
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				if (request.eventId() != null && !request.eventId().isEmpty()) {
					// 查询单个事件
					log.info("查询单个事件，事件ID: {}, 用户ID: {}", request.eventId(), userId);
					BookingDetails details = flightBookingService.getBookingDetailsByUserId(request.eventId(), userId);
					log.info("成功查询到事件详情");
					return List.of(details);
				} else {
					// 查询用户所有事件
					log.info("查询用户所有事件，用户ID: {}", userId);
					List<BookingDetails> bookings = flightBookingService.getBookingsByUserId(userId);
					log.info("成功查询到 {} 个事件", bookings.size());
					return bookings;
				}
			} catch (Exception e) {
				log.warn("查找用户日程失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return List.of();
			}
		};
	}

	@Bean
	@Description("获取日程详细信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				log.info("开始获取日程详细信息，事件ID: {}", request.eventId());
				
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用请求参数中的userId
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService获取日程详细信息，用户ID: {}, 事件ID: {}", userId, request.eventId());
				BookingDetails details = flightBookingService.getBookingDetailsByUserId(request.eventId(), userId);
				log.info("成功获取日程详细信息: {}", details);
				return details;
			}
			catch (Exception e) {
				log.warn("获取日程详细信息失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.eventId(), "Unknown User", null, null, null, null, null, null);
			}
		};
	}

	public record BookingDetailsRequest(String eventId, Long userId) {
	}

	@Bean
	@Description("修改日程的信息")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			try {
				log.info("开始修改日程，事件ID: {}, 日期: {}, 开始时间: {}, 结束时间: {}", 
						request.eventId(), request.date(), request.startTime(), request.endTime());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService修改日程，用户ID: {}, 事件ID: {}", userId, request.eventId());
				
				// 先进行冲突检测
				try {
					log.info("开始冲突检测，用户ID: {}, 日期: {}, 时间: {} - {}", userId, request.date(), request.startTime(), request.endTime());
					ConflictCheckRequest conflictRequest = new ConflictCheckRequest();
					conflictRequest.setEventTitle("修改的日程");
					conflictRequest.setProposedDate(LocalDate.parse(request.date()));
					conflictRequest.setStartTime(LocalTime.parse(request.startTime()));
					conflictRequest.setEndTime(LocalTime.parse(request.endTime()));
					conflictRequest.setLocation("修改位置");
					conflictRequest.setDescription("修改描述");
					ConflictCheckResponse conflictResponse = conflictDetectionService.checkConflict(conflictRequest, userId);
					
					if (conflictResponse.isHasConflict()) {
						log.warn("检测到冲突，冲突数量: {}, 严重程度: {}", 
								conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
						String conflictMessage = String.format("检测到%d个冲突事件，严重程度: %s。",
								conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
						
						if (conflictResponse.getSuggestions() != null && !conflictResponse.getSuggestions().isEmpty()) {
							conflictMessage += " 建议时间: ";
							for (TimeSuggestion suggestion : conflictResponse.getSuggestions()) {
								conflictMessage += String.format("%s %s-%s (置信度: %.0f%%) ", 
										suggestion.getDate(), suggestion.getStartTime(), suggestion.getEndTime(),
										suggestion.getConfidence() * 100);
							}
						}
						
						log.info("返回冲突检测结果: {}", conflictMessage);
						return "日程修改失败: " + conflictMessage;
					}
					
					log.info("未检测到冲突，继续修改日程");
				} catch (Exception e) {
					log.error("冲突检测失败，继续修改日程: {}", e.getMessage(), e);
				}
				
				flightBookingService.changeBookingByUserId(request.eventId(), userId, request.date(), request.startTime(),
						request.endTime());
				log.info("日程修改成功");
				return "日程修改成功";
			} catch (Exception e) {
				log.error("修改日程失败: ", e);
				return "日程修改失败: " + e.getMessage();
			}
		};
	}

	public record ChangeBookingDatesRequest(String eventId, Long userId, String date, String startTime, String endTime) {
	}

	// 在 BookingTools 类中添加
	public record CreateBookingRequest(String date, String startTime, String endTime, String title, String description, String location, String timezone, String priority, Boolean isAllDay, String eventType) {
		// 简化构造函数，只传必要字段
		public CreateBookingRequest(String date, String startTime, String endTime, String title) {
			this(date, startTime, endTime, title, null, null, "UTC", "MEDIUM", false, "MEETING");
		}
		
		// 包含描述的构造函数
		public CreateBookingRequest(String date, String startTime, String endTime, String title, String description) {
			this(date, startTime, endTime, title, description, null, "UTC", "MEDIUM", false, "MEETING");
		}
	}

	// 更新 createBooking Bean 方法
	@Bean
	@Description("创建日程")
	public Function<CreateBookingRequest, String> createBooking() {
		return request -> {
			try {
				log.info("开始创建日程，请求参数: date={}, startTime={}, endTime={}, title={}", 
						request.date(), request.startTime(), request.endTime(), request.title());
				
				// 获取用户ID
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用默认用户ID
				if (userId == null) {
					userId = 3L; // 默认用户ID
					log.warn("无法从认证上下文获取用户ID，使用默认用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}

				// 处理标题
				String title = request.title();
				if (title == null || title.trim().isEmpty()) {
					title = "会议: " + (request.startTime() != null ? request.startTime() : "未指定地点");
					log.info("标题为空，自动生成标题: {}", title);
				}

				// 处理描述
				String description = request.description();
				if (description == null || description.trim().isEmpty()) {
					description = request.endTime(); // 如果没有提供描述，使用endTime字段作为描述
					log.info("描述为空，使用endTime字段作为描述: {}", description);
				}

				// 处理地点
				String location = request.location();
				if (location == null || location.trim().isEmpty()) {
					location = request.startTime(); // 如果没有提供地点，使用startTime字段作为地点
					log.info("地点为空，使用startTime字段作为地点: {}", location);
				}

				// 处理时区
				String timezone = request.timezone();
				if (timezone == null || timezone.trim().isEmpty()) {
					timezone = "UTC"; // 默认时区
					log.info("时区为空，使用默认时区: {}", timezone);
				}

				log.info("调用FlightBookingService创建日程，参数: userId={}, date={}, location={}, description={}, title={}, timezone={}",
						userId, request.date(), location, description, title, timezone);

				// 先进行冲突检测
				try {
					log.info("开始冲突检测，用户ID: {}, 日期: {}, 时间: {} - {}", userId, request.date(), request.startTime(), request.endTime());
					ConflictCheckRequest conflictRequest = new ConflictCheckRequest();
					conflictRequest.setEventTitle(title);
					conflictRequest.setProposedDate(LocalDate.parse(request.date()));
					conflictRequest.setStartTime(LocalTime.parse(request.startTime()));
					conflictRequest.setEndTime(LocalTime.parse(request.endTime()));
					conflictRequest.setLocation(location);
					conflictRequest.setDescription(description);
					ConflictCheckResponse conflictResponse = conflictDetectionService.checkConflict(conflictRequest, userId);
					
					if (conflictResponse.isHasConflict()) {
						log.warn("检测到冲突，冲突数量: {}, 严重程度: {}", 
								conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
						String conflictMessage = String.format("检测到%d个冲突事件，严重程度: %s。",
								conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
						
						if (conflictResponse.getSuggestions() != null && !conflictResponse.getSuggestions().isEmpty()) {
							conflictMessage += " 建议时间: ";
							for (TimeSuggestion suggestion : conflictResponse.getSuggestions()) {
								conflictMessage += String.format("%s %s-%s (置信度: %.0f%%) ", 
										suggestion.getDate(), suggestion.getStartTime(), suggestion.getEndTime(),
										suggestion.getConfidence() * 100);
							}
						}
						
						log.info("返回冲突检测结果: {}", conflictMessage);
						return "日程创建失败: " + conflictMessage;
					}
					
					log.info("未检测到冲突，继续创建日程");
				} catch (Exception e) {
					log.error("冲突检测失败，继续创建日程: {}", e.getMessage(), e);
				}

				flightBookingService.createBooking(
						request.date(),
						location,
						description,
						title,
						userId,
						timezone
				);
				log.info("日程创建成功");
				return "日程创建成功";
			} catch (Exception e) {
				log.error("创建日程失败: ", e);
				return "日程创建失败: " + e.getMessage();
			}
		};
	}

	public record AllBookingsRequest(Long userId) {
		// 添加默认构造函数
		public AllBookingsRequest() {
			this(null);
		}
	}
	
	public record SmartScheduleRequest(String date, Integer duration, String eventType, String location) {
		public SmartScheduleRequest(String date, Integer duration) {
			this(date, duration, "meeting", "office");
		}
		
		public SmartScheduleRequest(String date, Integer duration, String eventType) {
			this(date, duration, eventType, "office");
		}
	}
	
	@Bean
	@Description("获取所有日程")
	public Function<AllBookingsRequest, List<BookingDetails>> getAllBookings() {
		return request -> {
			try {
				System.out.println("开始获取所有日程 - 线程: " + Thread.currentThread().getName());
				
				Long userId = null;
				
				// 1. 首先尝试从请求参数获取用户ID
				if (request.userId() != null) {
					userId = request.userId();
					System.out.println("从请求参数获取用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
				}
				
				// 2. 如果请求中没有用户ID，尝试从当前请求ThreadLocal获取
				if (userId == null) {
					userId = currentRequestUserId.get();
					if (userId != null) {
						System.out.println("从当前请求ThreadLocal获取用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
					}
				}
				
				// 3. 如果还是无法获取，尝试从全局ThreadLocal获取
				if (userId == null) {
					userId = getCurrentUserId();
					if (userId != null) {
						System.out.println("从全局ThreadLocal获取用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
					}
				}
				
				// 4. 如果还是无法获取，使用默认用户ID
				if (userId == null) {
					userId = 3L;
					System.out.println("无法获取用户ID，使用默认用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
				}
				
				System.out.println("最终使用用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
				System.out.println("调用FlightBookingService获取用户所有日程，用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
				List<BookingDetails> bookings = flightBookingService.getBookingsByUserId(userId);
				System.out.println("成功获取到 " + bookings.size() + " 个日程 - 线程: " + Thread.currentThread().getName());
				return bookings;
			} catch (Exception e) {
				System.out.println("获取所有日程失败: " + NestedExceptionUtils.getMostSpecificCause(e).getMessage() + " - 线程: " + Thread.currentThread().getName());
				return List.of();
			}
		};
	}
	
	@Bean
	@Description("获取智能日程建议")
	public Function<SmartScheduleRequest, String> getSmartScheduleSuggestions() {
		return request -> {
			try {
				log.info("开始获取智能日程建议，请求参数: date={}, duration={}, eventType={}, location={}", 
						request.date(), request.duration(), request.eventType(), request.location());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = 3L; // 默认用户ID
					log.warn("无法从认证上下文获取用户ID，使用默认用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				// 创建智能建议请求
				SmartSuggestionsRequest smartRequest =
						new SmartSuggestionsRequest();
				smartRequest.setDate(LocalDate.parse(request.date()));
				smartRequest.setDuration(request.duration());
				smartRequest.setEventType(request.eventType());
				smartRequest.setLocation(request.location());
				
				log.info("调用ConflictDetectionService获取智能建议");
				SmartSuggestionsResponse response =
						conflictDetectionService.getSmartSuggestions(smartRequest, userId);
				
				StringBuilder result = new StringBuilder();
				result.append("智能日程建议:\n");
				result.append("建议日期: ").append(response.getDate()).append("\n");
				result.append("最佳时间段:\n");
				
				if (response.getOptimalSlots() != null && !response.getOptimalSlots().isEmpty()) {
					result.append("\n最佳时间段建议：\n");
					response.getOptimalSlots().forEach(slot -> {
						result.append(String.format("- %s 到 %s (置信度: %.0f%%)\n", 
							slot.getStartTime(), slot.getEndTime(), slot.getConfidence() * 100));
					});
				} else {
					result.append("- 暂无可用时间段\n");
				}
				
				if (response.getMessage() != null && !response.getMessage().isEmpty()) {
					result.append("提示信息: ").append(response.getMessage()).append("\n");
				}
				
				log.info("成功获取智能建议: {}", result.toString());
				return result.toString();
				
			} catch (Exception e) {
				log.error("获取智能日程建议失败: ", e);
				return "获取智能日程建议失败: " + e.getMessage();
			}
		};
	}

	/**
	 * 获取当前登录用户的ID
	 * @return 当前用户ID，如果无法获取返回null
	 */
	private Long getCurrentUserId() {
		System.out.println("开始获取当前用户ID - 线程: " + Thread.currentThread().getName());
		
		// 首先尝试从当前请求的ThreadLocal获取用户ID
		Long requestUserId = currentRequestUserId.get();
		System.out.println("当前请求ThreadLocal中的用户ID: " + requestUserId + " - 线程: " + Thread.currentThread().getName());
		if (requestUserId != null) {
			System.out.println("从当前请求ThreadLocal获取到用户ID: " + requestUserId + " - 线程: " + Thread.currentThread().getName());
			return requestUserId;
		}
		
		// 其次尝试从全局ThreadLocal获取用户ID（来自AI调用链）
		Long threadLocalUserId = UserContextHolder.getCurrentUserId();
		System.out.println("全局ThreadLocal中的用户ID: " + threadLocalUserId + " - 线程: " + Thread.currentThread().getName());
		if (threadLocalUserId != null) {
			System.out.println("从全局ThreadLocal获取到用户ID: " + threadLocalUserId + " - 线程: " + Thread.currentThread().getName());
			return threadLocalUserId;
		}
		
		// 最后尝试从SecurityContext获取用户ID（来自HTTP请求）
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			System.out.println("尝试从SecurityContext获取认证信息，认证对象: " + authentication + " - 线程: " + Thread.currentThread().getName());
			
			if (authentication != null && authentication.isAuthenticated() && 
				authentication.getPrincipal() instanceof UserDetailsImpl) {
				UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
				System.out.println("从SecurityContext获取到用户ID: " + userDetails.getId() + " - 线程: " + Thread.currentThread().getName());
				return userDetails.getId();
			}
		} catch (Exception e) {
			System.err.println("从SecurityContext获取用户ID失败: " + e.getMessage() + " - 线程: " + Thread.currentThread().getName());
		}
		
		System.out.println("SecurityContext中未找到认证信息 - 线程: " + Thread.currentThread().getName());
		return null;
	}
}