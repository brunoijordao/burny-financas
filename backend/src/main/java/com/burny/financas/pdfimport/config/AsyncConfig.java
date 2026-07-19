package com.burny.financas.pdfimport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded executor for PDF-import processing (extraction + Gemma call), kept off the request thread
 * (see design.md "Async processing, not a synchronous request/response upload"). Always a genuine
 * background thread, including under the {@code test} profile: swapping in a same-thread executor
 * for tests was tried and reverted — invoking a new {@code @Transactional} method (as
 * {@code PdfImportProcessingListener} does) from within a {@code @TransactionalEventListener(AFTER_COMMIT)}
 * callback on the *same* thread as the just-committed outer transaction hits stale
 * {@code TransactionSynchronizationManager} thread-local state and silently fails to commit. Running
 * on a real (if small) thread pool sidesteps that entirely, in both tests and production; tests await
 * the outcome by polling instead of assuming synchronous completion.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "pdfImportTaskExecutor")
    public TaskExecutor pdfImportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("pdf-import-");
        executor.initialize();
        return executor;
    }
}
