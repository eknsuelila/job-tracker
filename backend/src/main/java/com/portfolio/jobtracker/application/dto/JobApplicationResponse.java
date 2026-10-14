package com.portfolio.jobtracker.application.dto;

import com.portfolio.jobtracker.application.ApplicationStatus;
import com.portfolio.jobtracker.application.JobApplication;

import java.time.Instant;
import java.time.LocalDate;

/** API representation of a job application. Entities are never returned directly. */
public record JobApplicationResponse(
        Long id,
        String company,
        String position,
        String location,
        String jobUrl,
        Integer salary,
        ApplicationStatus status,
        LocalDate appliedDate,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static JobApplicationResponse from(JobApplication a) {
        return new JobApplicationResponse(a.getId(), a.getCompany(), a.getPosition(), a.getLocation(),
                a.getJobUrl(), a.getSalary(), a.getStatus(), a.getAppliedDate(), a.getNotes(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
