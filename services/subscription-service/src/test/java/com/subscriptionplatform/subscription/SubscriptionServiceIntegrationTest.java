package com.subscriptionplatform.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionplatform.subscription.api.dto.CancelRequest;
import com.subscriptionplatform.subscription.api.dto.ReactivateRequest;
import com.subscriptionplatform.subscription.api.dto.StartTrialRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SubscriptionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("subscription_test")
            .withUsername("platform")
            .withPassword("platform_secret");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getSubscription_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/subscriptions/me").param("userId", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void startTrial_createsSubscriptionAndReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        StartTrialRequest request = new StartTrialRequest(userId, 14);

        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("TRIALING"))
                .andExpect(jsonPath("$.plan").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.trialEndsAt").exists());
    }

    @Test
    void getSubscription_afterTrial_returnsSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        StartTrialRequest request = new StartTrialRequest(userId, 14);

        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/subscriptions/me").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("TRIALING"));
    }

    @Test
    void cancel_atPeriodEnd_succeeds() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartTrialRequest(userId, 14))))
                .andExpect(status().isOk());

        CancelRequest cancelRequest = new CancelRequest(userId, true);
        mockMvc.perform(post("/subscriptions/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true));
    }

    @Test
    void cancel_immediate_succeeds() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartTrialRequest(userId, 14))))
                .andExpect(status().isOk());

        CancelRequest cancelRequest = new CancelRequest(userId, false);
        mockMvc.perform(post("/subscriptions/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void reactivate_afterCancel_succeeds() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartTrialRequest(userId, 14))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/subscriptions/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelRequest(userId, false))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/subscriptions/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReactivateRequest(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void startTrial_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trialDays\": 14}"))
                .andExpect(status().isBadRequest());
    }
}
