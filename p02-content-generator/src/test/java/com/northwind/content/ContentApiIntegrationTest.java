package com.northwind.content;

import com.embabel.agent.core.support.InvalidLlmReturnTypeException;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.content.review.ReviewAnalysis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class ContentApiIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void invalidBriefIsRejectedBeforeAnyModelCall() {
        assertThat(mvc.post().uri("/api/copy").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku":"TENT-2P","name":"","category":"tents","features":[],"audience":"hikers"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions();
    }

    @Test
    void reviewAnalysisReturnsTypedJson() {
        whenCreateObject(prompt -> prompt.contains("<review>"), ReviewAnalysis.class)
                .thenReturn(new ReviewAnalysis(ReviewAnalysis.Sentiment.POSITIVE, 5,
                        List.of(ReviewAnalysis.Topic.QUALITY, ReviewAnalysis.Topic.DURABILITY),
                        "en", "Tent survived a storm.", false));

        assertThat(mvc.post().uri("/api/reviews/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"text":"Survived a mountain storm, still bone dry inside. Five stars."}"""))
                .hasStatusOk()
                .bodyJson().extractingPath("$.topics").asArray().containsExactly("QUALITY", "DURABILITY");
    }

    @Test
    void persistentlyInvalidModelOutputBecomes502WithoutLeakingIt() {
        whenCreateObject(prompt -> prompt.contains("<review>"), ReviewAnalysis.class)
                .thenThrow(new InvalidLlmReturnTypeException("garbage", Set.of()));

        assertThat(mvc.post().uri("/api/reviews/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"text":"Great boots."}"""))
                .hasStatus(HttpStatus.BAD_GATEWAY)
                .bodyJson().extractingPath("$.title").isEqualTo("Generation failed");
    }
}
