package com.portfolio.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the backend.
 *
 * <p>{@code @SpringBootApplication} combines three annotations:
 * <ul>
 *   <li>{@code @Configuration} - this class may declare beans;</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring Boot configures things automatically based on the
 *       libraries on the classpath (e.g. it sees spring-boot-starter-web and starts an embedded Tomcat
 *       server, it sees H2 + JPA and creates a DataSource and an EntityManager);</li>
 *   <li>{@code @ComponentScan} - every class in this package and its sub-packages annotated with
 *       {@code @Component}, {@code @Service}, {@code @RestController}, {@code @Repository} or
 *       {@code @Configuration} is discovered and created as a Spring "bean".</li>
 * </ul>
 * That is why this class lives in the ROOT package: everything else is below it.
 */
@SpringBootApplication
public class JobTrackerApplication {

    public static void main(String[] args) {
        // Starts the Spring container, runs Flyway migrations, then starts Tomcat on port 8080.
        SpringApplication.run(JobTrackerApplication.class, args);
    }
}
