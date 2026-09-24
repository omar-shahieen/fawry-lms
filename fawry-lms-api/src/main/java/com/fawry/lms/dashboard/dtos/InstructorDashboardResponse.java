package com.fawry.lms.dashboard.dtos;

import java.util.List;

public record InstructorDashboardResponse(
        List<InstructorCourseSummaryResponse> courses,
        List<InstructorAnnouncementResponse> announcements) {
}
