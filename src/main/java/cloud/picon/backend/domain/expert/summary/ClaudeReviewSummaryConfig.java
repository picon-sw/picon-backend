package cloud.picon.backend.domain.expert.summary;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClaudeReviewSummaryProperties.class)
class ClaudeReviewSummaryConfig {

    @Bean
    ClaudeReviewSummaryClient claudeReviewSummaryClient(ClaudeReviewSummaryProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        var client = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(factory)
                .build();
        return new ClaudeReviewSummaryClient(client, properties);
    }

    @Bean
    ReviewSummaryService reviewSummaryService(ClaudeReviewSummaryClient client,
                                              ClaudeReviewSummaryProperties properties, JsonMapper jsonMapper) {
        return new ReviewSummaryService(client, properties, jsonMapper);
    }
}
