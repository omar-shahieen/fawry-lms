package com.fawry.lms.course.dtos;

import java.util.UUID;
import jakarta.validation.constraints.Pattern;

public record UpdateCourseRequest(
        @Pattern(regexp = ".*\\S.*") String title,
        String description,
        @Pattern(regexp = ".*\\S.*") String code,
        @Pattern(regexp = ".*\\S.*") String term,
        UUID instructorId) {
}
