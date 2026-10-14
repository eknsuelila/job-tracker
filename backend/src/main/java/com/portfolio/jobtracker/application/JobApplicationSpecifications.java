package com.portfolio.jobtracker.application;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable query filters for the list/search endpoint.
 *
 * <p>A {@link Specification} is one piece of a WHERE clause, built with the JPA Criteria API:
 * {@code root} is the entity being queried, {@code cb} (CriteriaBuilder) creates conditions.
 * Specifications can be combined with {@code .and()} / {@code .or()}, and returning {@code null}
 * means "no condition", so optional filters are simply skipped.
 *
 * <p>Example: {@code ownedBy(1).and(hasStatus(INTERVIEW)).and(matches("acme"))} becomes roughly
 * {@code WHERE user_id = 1 AND status = 'INTERVIEW' AND (lower(company) LIKE '%acme%' OR ...)}.
 *
 * <p>The alternative, writing one JPQL query per filter combination, quickly explodes in number.
 */
final class JobApplicationSpecifications {

    private JobApplicationSpecifications() {
    }

    static Specification<JobApplication> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Case-insensitive match on company, position or location. */
    static Specification<JobApplication> matches(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("company")), pattern),
                    cb.like(cb.lower(root.get("position")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern));
        };
    }
}
