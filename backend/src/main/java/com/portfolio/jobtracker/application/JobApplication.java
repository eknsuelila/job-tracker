package com.portfolio.jobtracker.application;

import com.portfolio.jobtracker.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One job the user applied for (or plans to apply for). Mapped to the {@code job_applications} table.
 *
 * <p>Entities represent DATABASE rows and are never sent to the browser directly. The API returns
 * {@code JobApplicationResponse} DTOs instead, which (1) hides internal fields such as the owning user,
 * (2) prevents accidental lazy-loading during JSON serialization, and (3) lets the database model
 * change without breaking the API contract.
 */
@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The owner. Many applications belong to one user -> {@code @ManyToOne}.
     * {@code LAZY} means the user row is NOT loaded from the database until getUser() is actually used,
     * avoiding an extra query every time an application is read.
     * {@code @JoinColumn} names the foreign-key column (user_id) in this table.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(nullable = false, length = 150)
    private String position;

    @Column(length = 150)
    private String location;

    @Column(name = "job_url", length = 500)
    private String jobUrl;

    /** Yearly salary in whole dollars. Integer (not int) because it is optional and may be null. */
    private Integer salary;

    /**
     * {@code EnumType.STRING} stores the enum NAME ("INTERVIEW") rather than its position (3).
     * Storing positions is fragile: inserting a new status in the middle of the enum would silently
     * change the meaning of every existing row.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
    }

    public JobApplication(User user) {
        this.user = user;
    }

    /**
     * JPA lifecycle callbacks: Hibernate calls {@code @PrePersist} right before the INSERT and
     * {@code @PreUpdate} right before an UPDATE, so timestamps are always maintained automatically.
     */
    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }
    public Integer getSalary() { return salary; }
    public void setSalary(Integer salary) { this.salary = salary; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
