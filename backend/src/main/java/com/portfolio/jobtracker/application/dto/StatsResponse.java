package com.portfolio.jobtracker.application.dto;

import com.portfolio.jobtracker.application.ApplicationStatus;

import java.util.List;
import java.util.Map;

/**
 * Dashboard numbers.
 *
 * @param responseRate share of submitted applications (excluding wishlist) that progressed past "applied", 0-100
 */
public record StatsResponse(long total,
                            Map<ApplicationStatus, Long> byStatus,
                            List<WeeklyCount> perWeek,
                            double responseRate) {
}
