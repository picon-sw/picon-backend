package cloud.picon.backend.domain.expert;

import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.LocalFakeImageUrlProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=classpath:application-test.yaml",
        "spring.datasource.url=jdbc:h2:mem:fake-image-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
        "S3_FAKE_IMAGE_URL=true",
        "app.s3.bucket=",
        "app.s3.region="
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LocalFakeImageIntegrationTest {

    @Autowired ApplicationContext context;
    @Autowired S3ImageUrlProvider provider;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired JdbcTemplate jdbc;

    @Test
    void localApiUsesFakeProviderThroughEnvironmentFlagWithoutAwsConfiguration() throws Exception {
        assertThat(provider).isInstanceOf(LocalFakeImageUrlProvider.class);
        assertThat(context.getBeansOfType(S3Presigner.class)).isEmpty();
        jdbc.update("""
                INSERT INTO member(member_id, nickname, created_at, updated_at)
                VALUES (1, 'local-expert', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO expert(member_id, created_at, updated_at)
                VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO expert_image(expert_image_id, member_id, image_key, specialty, created_at, updated_at)
                VALUES (1, 1, 'portfolio/a b?#.jpg', 'SHOOTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        mvc.perform(get("/api/portfolio-images")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.content[0].imageUrl")
                        .value("https://local.invalid/portfolio/a%20b%3F%23.jpg"));
    }
}
