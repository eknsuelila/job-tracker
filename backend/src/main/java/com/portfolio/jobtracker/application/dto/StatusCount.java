package com.portfolio.jobtracker.application.dto;

import com.portfolio.jobtracker.application.ApplicationStatus;

public record StatusCount(ApplicationStatus status, long count) {
}
