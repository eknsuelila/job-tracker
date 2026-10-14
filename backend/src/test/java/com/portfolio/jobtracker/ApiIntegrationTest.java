package com.portfolio.jobtracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION test: starts the whole Spring application (security, controllers, services, JPA,
 * Flyway, in-memory H2 database) and sends real HTTP requests through MockMvc.
 *
 * This proves that all layers work together: JSON parsing, validation, JWT security,
 * database queries and error handling. Demo data is turned off so each test controls its own data.
 */
@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    /** Registers a brand-new user (unique email per test) and returns their JWT. */
    private String registerAndGetToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"name": "Jane Doe", "email": "%s", "password": "supersecret"}
                """.formatted(email);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void protectedEndpoints_requireAToken() throws Exception {
        mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/applications").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        registerAndGetToken();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void fullCrudFlow() throws Exception {
        String token = registerAndGetToken();

        // CREATE
        String created = mvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme", "position": "Java Developer", "appliedDate": "2026-01-15",
                                 "salary": 70000, "jobUrl": "https://acme.example.com/jobs/1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // READ (list + search)
        mvc.perform(get("/api/applications").param("q", "acm").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Acme"));

        // PATCH status
        mvc.perform(patch("/api/applications/{id}/status", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        // Filter by status
        mvc.perform(get("/api/applications").param("status", "INTERVIEW").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1));

        // STATS
        mvc.perform(get("/api/applications/stats").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.byStatus.INTERVIEW").value(1))
                .andExpect(jsonPath("$.perWeek", hasSize(8)));

        // DELETE, then it is gone
        mvc.perform(delete("/api/applications/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersApplications() throws Exception {
        String alice = registerAndGetToken();
        String bob = registerAndGetToken();

        String created = mvc.perform(post("/api/applications")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Secret Co\",\"position\":\"Dev\",\"appliedDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // Bob gets 404 (not 403) so he can't even tell that the record exists.
        mvc.perform(get("/api/applications/{id}", id).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidInput_returnsFieldErrors() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"\",\"position\":\"Dev\",\"jobUrl\":\"ftp://x\",\"salary\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.company").exists())
                .andExpect(jsonPath("$.fieldErrors.jobUrl").exists())
                .andExpect(jsonPath("$.fieldErrors.salary").exists())
                .andExpect(jsonPath("$.fieldErrors.appliedDate").exists());
    }

    @Test
    void registeringTheSameEmailTwice_returns409() throws Exception {
        String body = "{\"name\":\"A\",\"email\":\"dupe@example.com\",\"password\":\"password1\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
}
