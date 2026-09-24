package com.fawry.lms.grade.dtos;

import java.util.UUID;

public record GradeStudentResponse(UUID id, String fullName, String email) {
}
