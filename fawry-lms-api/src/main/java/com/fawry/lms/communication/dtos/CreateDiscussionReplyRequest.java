package com.fawry.lms.communication.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateDiscussionReplyRequest(@NotBlank String body) {
}
