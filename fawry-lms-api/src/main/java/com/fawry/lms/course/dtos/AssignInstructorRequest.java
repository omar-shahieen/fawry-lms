package com.fawry.lms.course.dtos;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignInstructorRequest(@NotNull UUID instructorId) {
}
