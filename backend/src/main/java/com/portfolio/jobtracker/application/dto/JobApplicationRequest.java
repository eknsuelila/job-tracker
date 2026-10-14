package com.portfolio.jobtracker.application.dto;

import com.portfolio.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * JSON body for creating/updating an application.
 *
 * <p>A Java {@code record} is an immutable data class: the compiler generates the constructor,
 * accessors (company(), position()...), equals, hashCode and toString. Ideal for DTOs.
 *
 * <p>The annotations are Bean Validation (Jakarta Validation) rules, checked when the controller
 * parameter is marked {@code @Valid}:
 * <ul>
 *   <li>{@code @NotBlank} - not null and not only whitespace</li>
 *   <li>{@code @Size(max)} - matches the column length so the DB never truncates or errors</li>
 *   <li>{@code @Pattern} - jobUrl must be empty or start with http(s):// (blocks "javascript:" links)</li>
 *   <li>{@code @PositiveOrZero}, {@code @PastOrPresent}, {@code @NotNull} - self-explanatory</li>
 * </ul>
 * {@code status} is optional; the service defaults it to APPLIED.
 */
public record JobApplicationRequest(
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String position,
        @Size(max = 150) String location,
        @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "must start with http:// or https://") String jobUrl,
        @PositiveOrZero Integer salary,
        ApplicationStatus status,
        @NotNull @PastOrPresent LocalDate appliedDate,
        @Size(max = 2000) String notes) {
}
