package com.fawry.lms.dashboard.dtos;

import com.fawry.lms.user.entities.Role;

import java.util.Map;

public record AdminDashboardResponse(
        Map<Role, Long> userCountsByRole,
        long totalCourseCount,
        long totalEnrollmentCount) {
}
