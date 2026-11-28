package cloud.picon.backend.domain.expert.summary;

import cloud.picon.backend.domain.review.dto.ReviewCreateRequest;
import cloud.picon.backend.domain.review.service.ReviewRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:review-summary-refresh;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
        "app.claude.review-summary.refresh-enabled=true"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ExpertReviewSummaryRefreshIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ExpertReviewSummaryStore store;
    @Autowired ReviewRegistrationService registration;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JsonMapper mapper;
    @MockitoBean ReviewSummaryService claude;
    @MockitoBean(name = "reviewSummaryRefreshExecutor") ThreadPoolTaskExecutor executor;

    private final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
    private static final LocalDateTime PREVIOUS_UPDATE = LocalDateTime.of(2025, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        for (String table : List.of("review", "reservation", "proposal", "`request`", "expert", "member")) {
            jdbc.execute("DELETE FROM " + table);
        }
        jdbc.update("""
                INSERT INTO member(member_id,nickname,created_at,updated_at)
                VALUES (1,'Expert A',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,'Requester A',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                       (3,'Expert B',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO expert(member_id, career_years, avg_rating, review_count, review_summary,
                                   review_summary_count, review_summary_updated_at, created_at, updated_at)
                VALUES (1,0,0,0,'Previous summary',3,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                       (3,0,0,0,NULL,0,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, PREVIOUS_UPDATE);
        jobs.clear();
        doAnswer(invocation -> {
            jobs.add(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void doesNotCallClaudeWithFewerThanThreeEligibleBodies(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            seedReview(i, 1, "Review " + i);
        }
        postReview(50, "\t\u00a0");
        runJob();
        verifyNoInteractions(claude);
        assertExistingSummary();
    }

    @Test
    void savesSummaryAndMetadataAndReturnsItInTheExistingDetailApi() throws Exception {
        seedReview(1, 1, "Great photos");
        seedReview(2, 1, "Friendly service");
        seedReview(3, 3, "Another expert");
        seedReview(4, 1, null);
        seedReview(5, 1, "");
        seedReview(6, 1, " \u0085 ");
        seedReview(7, 1, "Contact me at someone@example.invalid");
        seedReview(8, 1, "촬영 후기와 함께 남긴 연락처는 ０１０\u00a0１２３４–５６７８입니다.");
        var expected = List.of("On time", "Friendly service", "Great photos");
        when(claude.summarize(expected)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new ReviewSummaryResult("ok", "Photos arrived on time.");
        });

        postReview(50, "On time");
        verifyNoInteractions(claude);
        runJob();

        verify(claude).summarize(expected);
        var updated = state();
        assertThat(updated.text()).isEqualTo("Photos arrived on time.");
        assertThat(updated.count()).isEqualTo(3);
        assertThat(updated.updatedAt()).isAfter(PREVIOUS_UPDATE);
        mvc.perform(get("/api/experts/1").with(jwt().jwt(token -> token.subject("2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reviewSummary").value("Photos arrived on time."))
                .andExpect(jsonPath("$.result.reviewSummaryCount").doesNotExist())
                .andExpect(jsonPath("$.result.reviewSummaryUpdatedAt").doesNotExist());
        verifyNoMoreInteractions(claude);
        assertThat(jobs).isEmpty();
    }

    @Test
    void executesTheCommittedRefreshOnTheConfiguredWorkerThread() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        when(claude.summarize(List.of("Third", "Second", "First")))
                .thenReturn(new ReviewSummaryResult("ok", "Worker summary."));
        var worker = new ReviewSummaryRefreshConfiguration().reviewSummaryRefreshExecutor();
        worker.initialize();
        try {
            var completed = new CompletableFuture<Thread>();
            doAnswer(invocation -> {
                Runnable job = invocation.getArgument(0);
                worker.execute(() -> {
                    try {
                        job.run();
                        completed.complete(Thread.currentThread());
                    } catch (Throwable failure) {
                        completed.completeExceptionally(failure);
                    }
                });
                return null;
            }).when(executor).execute(any(Runnable.class));

            postReview(50, "Third");

            var workerThread = completed.get(10, TimeUnit.SECONDS);
            assertThat(workerThread).isNotSameAs(Thread.currentThread());
            assertThat(workerThread.getName()).startsWith("review-summary-");
            assertThat(state().text()).isEqualTo("Worker summary.");
        } finally {
            worker.shutdown();
        }
    }

    @Test
    void usesOnlyNewestThirtyEligibleReviewBodiesAcrossSourcePages() throws Exception {
        for (int i = 1; i <= 35; i++) {
            seedReview(i, 1, "Review " + i);
        }
        for (int i = 36; i <= 140; i++) {
            seedReview(i, 1, null);
        }
        var expected = IntStream.iterate(35, i -> i - 1).limit(30).mapToObj(i -> "Review " + i).toList();
        when(claude.summarize(expected)).thenReturn(new ReviewSummaryResult("ok", "Recent reviews summarized."));

        postReview(150, "");
        runJob();

        verify(claude).summarize(expected);
        assertThat(state().count()).isEqualTo(30);
    }

    @Test
    void modelFailKeepsExistingSummary() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        when(claude.summarize(any())).thenReturn(new ReviewSummaryResult("fail", ""));
        postReview(50, "Third");
        runJob();
        verify(claude).summarize(List.of("Third", "Second", "First"));
        assertExistingSummary();
    }

    @Test
    void claudeTimeoutKeepsExistingSummaryAndCommittedReview() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        when(claude.summarize(any())).thenThrow(
                new ReviewSummaryException(ReviewSummaryException.Kind.TIMEOUT, "Test timeout"));
        postReview(50, "Third");
        runJob();
        assertExistingSummary();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review WHERE reservation_id = 50", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void duplicateAndInvalidReviewsDoNotSubmitRefresh() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        seedReview(3, 1, "Third");
        seedReservation(50, 1);

        mvc.perform(post("/api/reservations/{reservationId}/reviews", 3)
                        .with(jwt().jwt(token -> token.subject("2"))).contentType("application/json")
                        .content("{\"rating\":5,\"reviewContent\":\"Duplicate\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/reservations/{reservationId}/reviews", 50)
                        .with(jwt().jwt(token -> token.subject("2"))).contentType("application/json")
                        .content("{\"rating\":5,\"reviewTags\":[4],\"reviewContent\":\"Invalid tags\"}"))
                .andExpect(status().isBadRequest());

        verify(executor, never()).execute(any(Runnable.class));
        verifyNoInteractions(claude);
        assertThat(jobs).isEmpty();
        assertExistingSummary();
    }

    @Test
    void rollbackDoesNotSubmitRefresh() {
        seedReservation(50, 1);
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            registration.register(50, 2, new ReviewCreateRequest(mapper.readTree("5"), null, "Third"));
            assertThat(jobs).isEmpty();
            transaction.setRollbackOnly();
        });
        verify(executor, never()).execute(any(Runnable.class));
        verifyNoInteractions(claude);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review WHERE reservation_id = 50", Integer.class))
                .isZero();
    }

    @Test
    void coalescesRegistrationsAndDiscardsAnOlderResultBeforeSaving() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        when(claude.summarize(List.of("Third", "Second", "First"))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            postReview(51, "Fourth");
            postReview(52, "Fifth");
            assertThat(jobs).isEmpty();
            return new ReviewSummaryResult("ok", "Outdated result.");
        });
        when(claude.summarize(List.of("Fifth", "Fourth", "Third", "Second", "First"))).thenAnswer(invocation -> {
            assertExistingSummary();
            return new ReviewSummaryResult("ok", "Current result.");
        });
        postReview(50, "Third");
        runJob();
        verify(executor, times(1)).execute(any(Runnable.class));
        verify(claude, times(2)).summarize(any());
        assertThat(state().text()).isEqualTo("Current result.");
        assertThat(state().count()).isEqualTo(5);
    }

    @Test
    void excludesMaximumLengthNumericBodyAndStillHandlesTheNextRegistration() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        postReview(50, "7".repeat(2_000));
        runJob();
        verifyNoInteractions(claude);
        assertExistingSummary();

        var input = List.of("Third", "Second", "First");
        when(claude.summarize(input)).thenReturn(new ReviewSummaryResult("ok", "New summary."));
        postReview(51, "Third");
        runJob();
        verify(claude).summarize(input);
        assertThat(state().text()).isEqualTo("New summary.");
    }

    @Test
    void releasesExpertAfterFatalWorkerErrorAndLetsTheErrorPropagate() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        var error = new StackOverflowError("Synthetic worker error");
        when(claude.summarize(List.of("Third", "Second", "First"))).thenThrow(error);
        postReview(50, "Third");
        assertThat(jobs).hasSize(1);
        assertThatThrownBy(() -> jobs.remove().run()).isSameAs(error);
        assertExistingSummary();

        var input = List.of("Fourth", "Third", "Second", "First");
        when(claude.summarize(input)).thenReturn(new ReviewSummaryResult("ok", "Recovered summary."));
        postReview(51, "Fourth");
        runJob();
        verify(claude).summarize(input);
        assertThat(state().text()).isEqualTo("Recovered summary.");
    }

    @Test
    void releasesExpertAfterExecutorRejection() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        doThrow(new RejectedExecutionException("Synthetic queue saturation")).doAnswer(invocation -> {
            jobs.add(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));

        postReview(50, "Third");
        assertThat(jobs).isEmpty();
        verifyNoInteractions(claude);
        assertExistingSummary();

        var input = List.of("Fourth", "Third", "Second", "First");
        when(claude.summarize(input)).thenReturn(new ReviewSummaryResult("ok", "Recovered summary."));
        postReview(51, "Fourth");
        runJob();
        verify(executor, times(2)).execute(any(Runnable.class));
        verify(claude).summarize(input);
        assertThat(state().text()).isEqualTo("Recovered summary.");
    }

    @Test
    void yieldsAfterTwoPassesWhenMoreCommittedEventsArrive() throws Exception {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        when(claude.summarize(List.of("Third", "Second", "First"))).thenAnswer(invocation -> {
            postReview(51, "Fourth");
            return new ReviewSummaryResult("ok", "Stale result.");
        });
        when(claude.summarize(List.of("Fourth", "Third", "Second", "First"))).thenAnswer(invocation -> {
            postReview(52, "Fifth");
            return new ReviewSummaryResult("ok", "Also stale.");
        });
        when(claude.summarize(List.of("Fifth", "Fourth", "Third", "Second", "First")))
                .thenReturn(new ReviewSummaryResult("ok", "Latest summary."));

        postReview(50, "Third");
        jobs.remove().run();
        assertExistingSummary();
        verify(claude, times(2)).summarize(any());
        runJob();

        verify(claude, times(3)).summarize(any());
        assertThat(state().text()).isEqualTo("Latest summary.");
    }

    @Test
    void rejectsOverwritingAnotherSavedSummaryForTheSameInput() {
        seedReview(1, 1, "First");
        seedReview(2, 1, "Second");
        seedReview(3, 1, "Third");
        var snapshot = store.snapshot(1).orElseThrow();
        assertThat(store.saveIfCurrent(1, snapshot, "Already saved.")).isTrue();
        assertThat(store.saveIfCurrent(1, snapshot, "Older response.")).isFalse();
        assertThat(state().text()).isEqualTo("Already saved.");
    }

    private void seedReservation(int id, int expertId) {
        jdbc.update("""
                INSERT INTO `request` (request_id, member_id, service_type, title, created_at, updated_at)
                VALUES (?, 2, 'SHOOTING', 'Portrait', ?, ?)
                """, id, PREVIOUS_UPDATE, PREVIOUS_UPDATE);
        jdbc.update("""
                INSERT INTO proposal (proposal_id, request_id, member_id, price, created_at, updated_at)
                VALUES (?, ?, ?, 10000, ?, ?)
                """, id, id, expertId, PREVIOUS_UPDATE, PREVIOUS_UPDATE);
        jdbc.update("""
                INSERT INTO reservation (reservation_id, proposal_id, reservation_date, reservation_time,
                                         status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'COMPLETED', ?, ?)
                """, id, id, PREVIOUS_UPDATE.toLocalDate(), PREVIOUS_UPDATE.toLocalTime(),
                PREVIOUS_UPDATE, PREVIOUS_UPDATE);
    }

    private void seedReview(int id, int expertId, String body) {
        seedReservation(id, expertId);
        var time = PREVIOUS_UPDATE.plusSeconds(id);
        jdbc.update("""
                INSERT INTO review(reservation_id,rating,review_content,created_at,updated_at)
                VALUES (?,4,?,?,?)
                """, id, body, time, time);
    }

    private void postReview(int id, String body) throws Exception {
        seedReservation(id, 1);
        var request = mapper.createObjectNode().put("rating", 5).put("reviewContent", body);
        request.putArray("reviewTags").add("Not summary input");
        mvc.perform(post("/api/reservations/{reservationId}/reviews", id)
                        .with(jwt().jwt(token -> token.subject("2"))).contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("CREATED"));
    }

    private void runJob() {
        assertThat(jobs).hasSize(1);
        jobs.remove().run();
        assertThat(jobs).isEmpty();
    }

    private ExpertReviewSummaryStore.SummaryState state() {
        return store.snapshot(1).orElseThrow().existing();
    }

    private void assertExistingSummary() {
        assertThat(state()).isEqualTo(new ExpertReviewSummaryStore.SummaryState("Previous summary", 3, PREVIOUS_UPDATE));
    }
}
