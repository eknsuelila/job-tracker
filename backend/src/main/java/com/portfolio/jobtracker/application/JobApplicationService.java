package com.portfolio.jobtracker.application;

import com.portfolio.jobtracker.application.dto.*;
import com.portfolio.jobtracker.common.PageResponse;
import com.portfolio.jobtracker.common.ResourceNotFoundException;
import com.portfolio.jobtracker.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.portfolio.jobtracker.application.JobApplicationSpecifications.*;

/**
 * Business logic for job applications: the layer between the controller (HTTP) and the
 * repository (database).
 *
 * <p><b>Security rule enforced here:</b> every method receives the logged-in {@link User} and only
 * ever touches that user's rows ({@link #findOwned}). Asking for someone else's id gives 404 Not Found,
 * so it isn't even possible to tell whether that id exists.
 *
 * <p>{@code @Transactional} on the class applies to every public method: each call runs in one
 * database transaction that is committed at the end, or rolled back if an exception escapes.
 * Read-only methods override it with {@code readOnly = true}, which lets Hibernate skip
 * dirty-checking (a small performance win) and documents intent.
 */
@Service
@Transactional
public class JobApplicationService {

    static final int WEEKS_IN_CHART = 8;

    /** Statuses that mean "the company responded positively". Used for the response-rate metric. */
    private static final Set<ApplicationStatus> PROGRESSED = EnumSet.of(
            ApplicationStatus.PHONE_SCREEN, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER);

    private final JobApplicationRepository repository;
    /** Source of "now". Injected so tests can fix the date (see JobApplicationServiceTest). */
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Paged, filtered, sorted list of the user's applications.
     *
     * @param status   optional status filter (null = all)
     * @param query    optional free text matched against company, position and location
     * @param pageable page number, page size and sort order, built by Spring from ?page=&size=&sort=
     */
    @Transactional(readOnly = true)
    public PageResponse<JobApplicationResponse> search(User user, ApplicationStatus status, String query,
                                                       Pageable pageable) {
        var spec = ownedBy(user.getId()).and(hasStatus(status)).and(matches(query));
        // findAll(spec, pageable) runs two queries: one for the page of rows (LIMIT/OFFSET) and a
        // COUNT(*) for totalElements. map() converts each entity into a response DTO.
        return PageResponse.from(repository.findAll(spec, pageable).map(JobApplicationResponse::from));
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse get(User user, Long id) {
        return JobApplicationResponse.from(findOwned(user, id));
    }

    public JobApplicationResponse create(User user, JobApplicationRequest request) {
        JobApplication application = new JobApplication(user);
        apply(application, request);
        return JobApplicationResponse.from(repository.save(application));
    }

    /**
     * Full update (HTTP PUT semantics: every field is replaced).
     * saveAndFlush() writes the UPDATE immediately so the returned updatedAt timestamp is current.
     */
    public JobApplicationResponse update(User user, Long id, JobApplicationRequest request) {
        JobApplication application = findOwned(user, id);
        apply(application, request);
        return JobApplicationResponse.from(repository.saveAndFlush(application));
    }

    /** Partial update (HTTP PATCH): only the status changes. Used by the dropdown in the list page. */
    public JobApplicationResponse updateStatus(User user, Long id, ApplicationStatus status) {
        JobApplication application = findOwned(user, id);
        application.setStatus(status);
        return JobApplicationResponse.from(repository.saveAndFlush(application));
    }

    public void delete(User user, Long id) {
        repository.delete(findOwned(user, id));
    }

    /**
     * Numbers for the dashboard: totals per status, a response rate, and applications per week.
     */
    @Transactional(readOnly = true)
    public StatsResponse stats(User user) {
        // Start every status at 0 so the frontend always receives all keys, even for unused statuses.
        // EnumMap keeps keys in enum declaration order (the pipeline order).
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s, 0L);
        }
        repository.countByStatus(user.getId()).forEach(c -> byStatus.put(c.status(), c.count()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long submitted = total - byStatus.get(ApplicationStatus.WISHLIST);
        long progressed = PROGRESSED.stream().mapToLong(byStatus::get).sum();
        // Percentage rounded to one decimal place; guard against division by zero for new users.
        double responseRate = submitted == 0 ? 0 : Math.round(progressed * 1000.0 / submitted) / 10.0;

        return new StatsResponse(total, byStatus, perWeek(user), responseRate);
    }

    /** Applications per week for the last {@link #WEEKS_IN_CHART} weeks, oldest first, including empty weeks. */
    private List<WeeklyCount> perWeek(User user) {
        // Weeks start on Monday. previousOrSame(MONDAY) maps any date to the Monday of its week.
        LocalDate thisWeek = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstWeek = thisWeek.minusWeeks(WEEKS_IN_CHART - 1L);

        // Pre-fill all 8 weeks with 0 so weeks without applications still appear as empty bars.
        // TreeMap keeps the keys sorted by date (oldest first).
        Map<LocalDate, Long> counts = new TreeMap<>();
        for (int i = 0; i < WEEKS_IN_CHART; i++) {
            counts.put(firstWeek.plusWeeks(i), 0L);
        }
        for (LocalDate date : repository.findAppliedDatesSince(user.getId(), firstWeek)) {
            counts.computeIfPresent(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    (week, count) -> count + 1);
        }
        return counts.entrySet().stream().map(e -> new WeeklyCount(e.getKey(), e.getValue())).toList();
    }

    /** Loads an application only if it belongs to this user; otherwise 404. The single ownership check. */
    private JobApplication findOwned(User user, Long id) {
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job application " + id + " not found"));
    }

    /** Copies request fields onto the entity, trimming text and storing empty strings as NULL. */
    private static void apply(JobApplication target, JobApplicationRequest r) {
        target.setCompany(r.company().trim());
        target.setPosition(r.position().trim());
        target.setLocation(blankToNull(r.location()));
        target.setJobUrl(blankToNull(r.jobUrl()));
        target.setSalary(r.salary());
        target.setStatus(r.status() != null ? r.status() : ApplicationStatus.APPLIED);
        target.setAppliedDate(r.appliedDate());
        target.setNotes(blankToNull(r.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
