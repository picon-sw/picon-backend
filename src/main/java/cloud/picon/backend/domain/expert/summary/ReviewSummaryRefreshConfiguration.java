package cloud.picon.backend.domain.expert.summary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.claude.review-summary.refresh-enabled", havingValue = "true")
class ReviewSummaryRefreshConfiguration {

    @Bean(defaultCandidate = false)
    ThreadPoolTaskExecutor reviewSummaryRefreshExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("review-summary-");
        return executor;
    }
}
