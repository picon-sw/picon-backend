package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.entity.Expert;
import cloud.picon.backend.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpertEntityIntegrationTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void sharedPrimaryKeyAndAllColumnsRoundTripWithAuditing() throws Exception {
        Member member = Member.createKakaoMember(987654L);
        entityManager.persist(member);
        var constructor = Expert.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Expert expert = constructor.newInstance();
        ReflectionTestUtils.setField(expert, "member", member);
        entityManager.persist(expert);
        entityManager.flush();

        assertThat(expert.getId()).isEqualTo(member.getId());
        assertThat(expert.getCareerYears()).isZero();
        assertThat(expert.getAvgRating()).isEqualByComparingTo("0.0");
        assertThat(expert.getReviewCount()).isZero();
        assertThat(expert.getReviewSummaryCount()).isZero();
        assertThat(expert.getReviewSummary()).isNull();
        assertThat(expert.getReviewSummaryUpdatedAt()).isNull();
        assertThat(expert.getCreatedAt()).isNotNull();
        assertThat(expert.getUpdatedAt()).isNotNull();

        LocalDateTime summaryTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        ReflectionTestUtils.setField(expert, "careerYears", (short) 7);
        ReflectionTestUtils.setField(expert, "avgRating", new BigDecimal("4.5"));
        ReflectionTestUtils.setField(expert, "reviewCount", 3);
        ReflectionTestUtils.setField(expert, "reviewSummary", "summary".repeat(1000));
        ReflectionTestUtils.setField(expert, "reviewSummaryCount", 2);
        ReflectionTestUtils.setField(expert, "reviewSummaryUpdatedAt", summaryTime);
        entityManager.flush();
        entityManager.clear();

        Expert loaded = entityManager.find(Expert.class, member.getId());
        assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
                .isLoaded(loaded, "member")).isFalse();
        assertThat(loaded.getCareerYears()).isEqualTo((short) 7);
        assertThat(loaded.getAvgRating()).isEqualByComparingTo("4.5");
        assertThat(loaded.getReviewCount()).isEqualTo(3);
        assertThat(loaded.getReviewSummary()).hasSize(7000);
        assertThat(loaded.getReviewSummaryCount()).isEqualTo(2);
        assertThat(loaded.getReviewSummaryUpdatedAt()).isEqualTo(summaryTime);
    }

    @Test
    void generatedTableHasExactlyErdColumnsAndDatabaseDefaults() {
        assertThat(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'EXPERT'
                """, String.class)).containsExactlyInAnyOrder("MEMBER_ID", "CAREER_YEARS",
                "AVG_RATING", "REVIEW_COUNT", "REVIEW_SUMMARY", "REVIEW_SUMMARY_COUNT",
                "REVIEW_SUMMARY_UPDATED_AT", "CREATED_AT", "UPDATED_AT");
        Member member = Member.createKakaoMember(987655L);
        entityManager.persist(member);
        entityManager.flush();
        jdbc.update("""
                INSERT INTO expert(member_id, created_at, updated_at)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, member.getId());
        entityManager.clear();
        Expert expert = entityManager.find(Expert.class, member.getId());
        assertThat(expert.getCareerYears()).isZero();
        assertThat(expert.getAvgRating()).isEqualByComparingTo("0.0");
        assertThat(expert.getReviewCount()).isZero();
        assertThat(expert.getReviewSummaryCount()).isZero();
    }
}
