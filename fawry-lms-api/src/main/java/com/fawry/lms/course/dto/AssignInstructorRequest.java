package com.fawry.lms.course.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignInstructorRequest(@NotNull UUID instructorId) {
}
