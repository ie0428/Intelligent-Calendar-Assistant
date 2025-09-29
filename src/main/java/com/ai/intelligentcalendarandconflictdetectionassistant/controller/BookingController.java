package com.ai.intelligentcalendarandconflictdetectionassistant.controller;


import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.FlightBookingService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@CrossOrigin
public class BookingController {

	private final FlightBookingService flightBookingService;

	public BookingController(FlightBookingService flightBookingService) {
		this.flightBookingService = flightBookingService;
	}
	@CrossOrigin
	@GetMapping(value = "/booking/list")
	public List<BookingDetails> getBookings() {
		Long userId = getCurrentUserId();
		return flightBookingService.getBookingsByUserId(userId);
	}

	/**
	 * 获取当前登录用户的ID
	 * @return 当前用户ID
	 * @throws SecurityException 如果用户未认证
	 */
	private Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			return userDetails.getId();
		}
		throw new SecurityException("用户未认证");
	}
}
