package com.portfolio.jobtracker.application.dto;

import java.time.LocalDate;

/** Number of applications sent in the week starting on {@code weekStart} (a Monday). */
public record WeeklyCount(LocalDate weekStart, long count) {
}
