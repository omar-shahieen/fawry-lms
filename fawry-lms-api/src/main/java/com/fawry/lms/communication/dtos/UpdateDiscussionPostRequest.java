package com.fawry.lms.communication.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateDiscussionPostRequest(String title, @NotBlank String body) {
}
