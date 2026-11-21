package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.dto.ExpertPhotoStyleResponse;
import cloud.picon.backend.domain.expert.dto.ExpertRegionResponse;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertDetailLegacyDataTest {

    @Test
    void duplicateRelationsAndOrphanedStylesKeepTheDetailContract() throws SQLException {
        // The current Entity schema prevents these rows. Use an isolated legacy schema to exercise the SQL.
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:expert-detail-legacy;MODE=MySQL;NON_KEYWORDS=MEMBER", "sa", "")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            var namedJdbc = new NamedParameterJdbcTemplate(jdbc);
            var detail = new ExpertDetailRepository(namedJdbc);
            var search = new ExpertSearchRepository(namedJdbc);

            jdbc.execute("""
                    CREATE TABLE expert (member_id BIGINT, career_years INT, avg_rating DECIMAL(2,1),
                        review_count INT, review_summary TEXT);
                    CREATE TABLE member (member_id BIGINT, nickname VARCHAR(20),
                        profile_image VARCHAR(100), introduction VARCHAR(100));
                    CREATE TABLE favorite (member_id BIGINT, expert_member_id BIGINT);
                    CREATE TABLE region (region_code BIGINT, first_depth VARCHAR(20),
                        second_depth VARCHAR(20), third_depth VARCHAR(20));
                    CREATE TABLE expert_region (member_id BIGINT, region_code BIGINT);
                    CREATE TABLE photo_style (photo_style_id BIGINT, name VARCHAR(20));
                    CREATE TABLE expert_photo_style (member_id BIGINT, photo_style_id BIGINT);
                    INSERT INTO expert VALUES (1, 3, 4.8, 23, NULL);
                    INSERT INTO member VALUES (1, 'expert', NULL, NULL);
                    INSERT INTO favorite VALUES (99, 1), (99, 1);
                    INSERT INTO region VALUES (1, 'Seoul', NULL, NULL);
                    INSERT INTO expert_region VALUES (1, 1), (1, 1);
                    INSERT INTO photo_style VALUES (10, 'Style A');
                    INSERT INTO expert_photo_style VALUES (1, 10), (1, 10), (1, 999);
                    """);

            assertThat(detail.findExpert(1, 99)).get().extracting(ExpertDetailRepository.ExpertDetailRow::favorite)
                    .isEqualTo(true);
            assertThat(search.findRegions(List.of(1L)).get(1L)).containsExactly(new ExpertRegionResponse(1L, "Seoul"));
            assertThat(detail.findPhotoStyles(1)).containsExactly(new ExpertPhotoStyleResponse(10L, "Style A"));
        }
    }
}
