package com.fawry.lms.communication.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateAnnouncementRequest(
        @NotBlank String title,
        @NotBlank String body) {
}
