package com.portfolio.jobtracker.config;

import com.portfolio.jobtracker.application.ApplicationStatus;
import com.portfolio.jobtracker.application.JobApplication;
import com.portfolio.jobtracker.application.JobApplicationRepository;
import com.portfolio.jobtracker.user.User;
import com.portfolio.jobtracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Creates a demo account with sample data so reviewers can try the app immediately. */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "password123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final JobApplicationRepository applications;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, JobApplicationRepository applications,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.applications = applications;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(new User("Demo User", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD)));
        LocalDate today = LocalDate.now();

        add(demo, "Acme Corp", "Junior Java Developer", "Remote", 72000, ApplicationStatus.INTERVIEW, today.minusDays(3));
        add(demo, "Globex", "Full Stack Developer", "New York, NY", 85000, ApplicationStatus.PHONE_SCREEN, today.minusDays(6));
        add(demo, "Initech", "Software Engineer I", "Austin, TX", 78000, ApplicationStatus.APPLIED, today.minusDays(8));
        add(demo, "Umbrella Health", "Angular Developer", "Remote", 80000, ApplicationStatus.REJECTED, today.minusDays(12));
        add(demo, "Stark Industries", "Backend Engineer (Spring)", "Los Angeles, CA", 95000, ApplicationStatus.OFFER, today.minusDays(20));
        add(demo, "Wayne Enterprises", "Associate Developer", "Chicago, IL", 70000, ApplicationStatus.APPLIED, today.minusDays(15));
        add(demo, "Hooli", "Graduate Software Engineer", "San Francisco, CA", 110000, ApplicationStatus.APPLIED, today.minusDays(27));
        add(demo, "Soylent", "Java Developer", "Remote", null, ApplicationStatus.WITHDRAWN, today.minusDays(33));
        add(demo, "Vandelay Industries", "Frontend Developer", "Seattle, WA", 76000, ApplicationStatus.WISHLIST, today);
        add(demo, "Cyberdyne", "Software Developer", "Denver, CO", 82000, ApplicationStatus.INTERVIEW, today.minusDays(40));

        log.info("Seeded demo account: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private void add(User user, String company, String position, String location, Integer salary,
                     ApplicationStatus status, LocalDate appliedDate) {
        JobApplication a = new JobApplication(user);
        a.setCompany(company);
        a.setPosition(position);
        a.setLocation(location);
        a.setSalary(salary);
        a.setStatus(status);
        a.setAppliedDate(appliedDate);
        a.setJobUrl("https://careers.example.com/" + company.toLowerCase().replaceAll("[^a-z]+", "-"));
        applications.save(a);
    }
}
