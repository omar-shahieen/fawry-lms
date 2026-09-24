package com.fawry.lms.communication.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDiscussionPostRequest(
        @Pattern(regexp = ".*\\S.*") String title,
        @Pattern(regexp = ".*\\S.*") String body) {
}
