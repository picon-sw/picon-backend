package cloud.picon.backend.domain.expert.summary;

import cloud.picon.backend.domain.auth.controller.AuthController;
import cloud.picon.backend.domain.auth.controller.RefreshTokenCookieProvider;
import cloud.picon.backend.domain.auth.service.AuthService;
import cloud.picon.backend.domain.expert.controller.ExpertController;
import cloud.picon.backend.domain.expert.service.ExpertDetailService;
import cloud.picon.backend.domain.expert.service.ExpertImageService;
import cloud.picon.backend.domain.expert.service.ExpertService;
import cloud.picon.backend.global.apipayload.handler.SecurityErrorHandler;
import cloud.picon.backend.global.config.JwtConfig;
import cloud.picon.backend.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClaudeReviewSummaryConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withInitializer(context -> {
                var sources = context.getEnvironment().getPropertySources();
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                new ConfigDataApplicationContextInitializer().initialize(context);
            })
            .withPropertyValues("spring.profiles.active=test")
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(ApiAndSecurityConfiguration.class)
            .withBean(AuthService.class, () -> mock(AuthService.class))
            .withBean(ExpertService.class, () -> mock(ExpertService.class))
            .withBean(ExpertDetailService.class, () -> mock(ExpertDetailService.class))
            .withBean(ExpertImageService.class, () -> mock(ExpertImageService.class));

    @Test
    void usesPackagedPromptAndRejectsMissingKeyWithApiAndSecurityBeansPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClaudeReviewSummaryProperties.class)
                    .hasSingleBean(ClaudeReviewSummaryClient.class)
                    .hasSingleBean(ReviewSummaryService.class)
                    .hasSingleBean(ExpertController.class)
                    .hasSingleBean(AuthController.class)
                    .hasSingleBean(JwtDecoder.class)
                    .hasBean("securityFilterChain")
                    .hasBean("swaggerFilterChain")
                    .doesNotHaveBean(DataSource.class)
                    .doesNotHaveBean(RedisConnectionFactory.class);
            assertThat(context.getBean("securityFilterChain")).isInstanceOf(SecurityFilterChain.class);

            var properties = context.getBean(ClaudeReviewSummaryProperties.class);
            assertThat(properties.prompt()).isEqualTo(new ClassPathResource("prompts/review-summary.txt"));
            assertThatThrownBy(() -> context.getBean(ReviewSummaryService.class).summarize(List.of("Great photos")))
                    .isInstanceOfSatisfying(ReviewSummaryException.class, exception -> {
                        assertThat(exception.kind()).isEqualTo(CONFIGURATION);
                        assertThat(exception).hasMessage("ANTHROPIC_API_KEY is required for review summarization");
                    });
        });
    }

    @Test
    void acceptsExternalPromptLocation(@TempDir Path tempDir) throws Exception {
        var prompt = Files.writeString(tempDir.resolve("prompt.txt"), "External test prompt");

        contextRunner.withPropertyValues("CLAUDE_REVIEW_SUMMARY_PROMPT=" + prompt.toUri()).run(context -> {
            assertThat(context).hasSingleBean(ReviewSummaryService.class);
            assertThat(context.getBean(ClaudeReviewSummaryProperties.class).prompt().getURI())
                    .isEqualTo(prompt.toUri());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({ClaudeReviewSummaryConfig.class, ExpertController.class, AuthController.class,
            RefreshTokenCookieProvider.class, SecurityErrorHandler.class, JwtConfig.class, SecurityConfig.class})
    static class ApiAndSecurityConfiguration {
    }
}
