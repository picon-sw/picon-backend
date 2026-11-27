package cloud.picon.backend.domain.expert.summary;

import cloud.picon.backend.domain.review.repository.ReviewRepository.ReviewBody;
import cloud.picon.backend.domain.review.service.ReviewRegistered;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.claude.review-summary.refresh-enabled", havingValue = "true")
class ExpertReviewSummaryRefresh {

    private static final int MIN_REVIEWS = 3;
    private static final int MAX_PASSES_PER_TASK = 2;

    private final ReviewSummaryService claude;
    private final ExpertReviewSummaryStore store;
    private final TaskExecutor executor;
    private final Map<Long, PendingRefresh> pending = new HashMap<>();

    private static final class PendingRefresh {
        private boolean changed;
        private List<ReviewBody> lastAttempt = List.of();
    }

    ExpertReviewSummaryRefresh(ReviewSummaryService claude, ExpertReviewSummaryStore store,
                               @Qualifier("reviewSummaryRefreshExecutor") TaskExecutor executor) {
        this.claude = claude;
        this.store = store;
        this.executor = executor;
    }

    @TransactionalEventListener
    public void afterRegistration(ReviewRegistered event) {
        long expertId = event.expertId();
        PendingRefresh job;
        synchronized (pending) {
            var existing = pending.get(expertId);
            if (existing != null) {
                existing.changed = true;
                return;
            }
            job = new PendingRefresh();
            pending.put(expertId, job);
        }
        submit(expertId, job);
    }

    private void submit(long expertId, PendingRefresh job) {
        boolean accepted = false;
        try {
            executor.execute(() -> refreshPending(expertId, job));
            accepted = true;
        } catch (RejectedExecutionException e) {
            log.warn("Review summary queue rejected expertId={}", expertId);
        } finally {
            if (!accepted) {
                synchronized (pending) {
                    pending.remove(expertId, job);
                }
            }
        }
    }

    private void refreshPending(long expertId, PendingRefresh job) {
        boolean completed = false;
        try {
            for (int pass = 0; pass < MAX_PASSES_PER_TASK; pass++) {
                synchronized (pending) {
                    job.changed = false;
                }
                refreshOnce(expertId, job);
                synchronized (pending) {
                    if (!job.changed) {
                        break;
                    }
                }
            }
            completed = true;
        } finally {
            finishTask(expertId, job, completed);
        }
    }

    private void finishTask(long expertId, PendingRefresh job, boolean completed) {
        synchronized (pending) {
            if (!completed || !job.changed) {
                pending.remove(expertId, job);
                return;
            }
        }
        // Yield the worker before processing further committed registrations.
        submit(expertId, job);
    }

    private void refreshOnce(long expertId, PendingRefresh job) {
        try {
            var snapshot = store.snapshot(expertId);
            if (snapshot.isEmpty()) {
                return;
            }
            var current = snapshot.get();
            if (current.input().size() < MIN_REVIEWS || job.lastAttempt.equals(current.input())) {
                return;
            }
            job.lastAttempt = current.input();
            var result = claude.summarize(job.lastAttempt.stream().map(ReviewBody::content).toList());
            if ("ok".equals(result.status())) {
                store.saveIfCurrent(expertId, current, result.summaryText());
            }
        } catch (RuntimeException e) {
            log.warn("Review summary refresh failed for expertId={}, type={}", expertId, e.getClass().getSimpleName());
        }
    }
}
