package com.portfolio.jobtracker.application.dto;

import com.portfolio.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull ApplicationStatus status) {
}
