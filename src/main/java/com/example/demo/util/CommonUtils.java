package com.example.demo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class CommonUtils {

	public static LocalDateTime getLocalDateTime() {
		ZoneId chennaiZone = ZoneId.of("Asia/Kolkata");

		// Get current date-time in Chennai, truncate to seconds
		ZonedDateTime chennaiZoned = ZonedDateTime.now(chennaiZone).truncatedTo(ChronoUnit.SECONDS);

		// Extract LocalDateTime (no offset, no zone)
		return chennaiZoned.toLocalDateTime();
	}
	
	public static final Set<String> ALLOWED_ROLES = Set.of("MANAGER", "LEAD", "MEMBER");

	public static boolean isValidRole(String role) {
	    return role != null && ALLOWED_ROLES.contains(role.toUpperCase());
	}
	
}
