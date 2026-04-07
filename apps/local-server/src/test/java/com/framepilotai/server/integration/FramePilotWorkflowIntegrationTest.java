package com.framepilotai.server.integration;

import com.framepilotai.server.web.dto.AgentBriefingRequest;
import com.framepilotai.server.web.dto.CreateProjectRequest;
import com.framepilotai.server.web.dto.ImportAssetsRequest;
import com.framepilotai.server.web.dto.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class FramePilotWorkflowIntegrationTest {

    private static final Path TEST_STORAGE_ROOT = createStorageRoot();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("framepilot.storage.root", () -> TEST_STORAGE_ROOT.toString());
    }

    @Test
    void shouldRunProjectWorkflowAndReturnCoordinatorBriefing() {
        ProjectResponse project = webTestClient.post()
                .uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProjectRequest("Integration Project", "Main flow integration test", "image-sequence"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectResponse.class)
                .returnResult()
                .getResponseBody();

        assert project != null;

        List<String> assetPaths = List.of(
                moduleRoot().resolve("../../storage/projects/demo-assets/sample-page-01.svg").normalize().toAbsolutePath().toString(),
                moduleRoot().resolve("../../storage/projects/demo-assets/sample-page-02.svg").normalize().toAbsolutePath().toString()
        );

        webTestClient.post()
                .uri("/api/projects/{projectId}/assets/import", project.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ImportAssetsRequest(assetPaths))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/projects/{projectId}/panels/parse", project.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.panelCount").isEqualTo(4);

        webTestClient.post()
                .uri("/api/projects/{projectId}/ocr/run", project.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.blockCount").isEqualTo(4);

        webTestClient.post()
                .uri("/api/projects/{projectId}/analysis/summary", project.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pipelineType").isNotEmpty()
                .jsonPath("$.preset").isNotEmpty();

        webTestClient.post()
                .uri("/api/projects/{projectId}/scene-plan", project.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sceneCount").isEqualTo(2);

        webTestClient.post()
                .uri("/api/agents/briefing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AgentBriefingRequest(project.id(), null, null, null, "Explain project complexity and preset."))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.advisories.length()").isEqualTo(4)
                .jsonPath("$.decisionAuthority").value(value -> ((String) value).contains("Rule engines"));
    }

    @Test
    void shouldRejectOcrBeforePanelParsing() {
        ProjectResponse project = webTestClient.post()
                .uri("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProjectRequest("Failure Project", "Failure path test", "image-sequence"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectResponse.class)
                .returnResult()
                .getResponseBody();

        assert project != null;

        webTestClient.post()
                .uri("/api/projects/{projectId}/ocr/run", project.id())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("No parsed panels found. Run panel parsing first.");
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("framepilot-it-");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create integration test storage root", exception);
        }
    }

    private static Path moduleRoot() {
        return Path.of("").toAbsolutePath().normalize();
    }
}
