package com.portfolio.jobtracker.application;

import com.portfolio.jobtracker.application.dto.*;
import com.portfolio.jobtracker.common.PageResponse;
import com.portfolio.jobtracker.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for job applications. Every endpoint requires a valid JWT (see SecurityConfig).
 *
 * <pre>
 * GET    /api/applications              list (filters + paging + sorting)
 * GET    /api/applications/stats        dashboard numbers
 * GET    /api/applications/{id}         one application
 * POST   /api/applications              create            -> 201 Created
 * PUT    /api/applications/{id}         replace all fields
 * PATCH  /api/applications/{id}/status  change only the status
 * DELETE /api/applications/{id}         delete            -> 204 No Content
 * </pre>
 *
 * <p>{@code @AuthenticationPrincipal User user} gives each method the logged-in user, which is
 * passed to the service so it can scope every query to that user.
 */
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    /**
     * Example: GET /api/applications?status=INTERVIEW&q=google&page=0&size=10&sort=appliedDate,desc
     *
     * <p>{@code @RequestParam(required = false)} reads optional query-string parameters. Spring converts
     * "INTERVIEW" to the enum automatically (an unknown value gives 400).
     * {@code Pageable} is built by Spring Data from page/size/sort; {@code @PageableDefault} supplies the
     * defaults when they're missing.
     */
    @GetMapping
    public PageResponse<JobApplicationResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 10, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(user, status, query, pageable);
    }

    // Declared before "/{id}" for readability. Spring would still match "/stats" correctly, because
    // literal paths take precedence over path variables.
    @GetMapping("/stats")
    public StatsResponse stats(@AuthenticationPrincipal User user) {
        return service.stats(user);
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse create(@AuthenticationPrincipal User user,
                                         @Valid @RequestBody JobApplicationRequest request) {
        return service.create(user, request);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                         @Valid @RequestBody JobApplicationRequest request) {
        return service.update(user, id, request);
    }

    /** {@code @PathVariable} binds the {id} part of the URL to the method parameter. */
    @PatchMapping("/{id}/status")
    public JobApplicationResponse updateStatus(@AuthenticationPrincipal User user, @PathVariable Long id,
                                               @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(user, id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
    }
}
