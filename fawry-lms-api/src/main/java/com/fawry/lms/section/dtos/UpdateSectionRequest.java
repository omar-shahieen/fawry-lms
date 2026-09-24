package com.fawry.lms.section.dtos;

import jakarta.validation.constraints.Pattern;

public record UpdateSectionRequest(@Pattern(regexp = ".*\\S.*") String title, Integer orderIndex) {
}
