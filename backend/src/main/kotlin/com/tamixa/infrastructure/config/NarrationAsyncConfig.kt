package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Bounded async executors for narration.
 * - narrationTtsExecutor: TTS API calls
 * - pipelineLanguageExecutor: parallel language processing within a story
 * - pipelineRewriteExecutor: dedicated pool for rewrite (OpenAI); avoids ForkJoinPool.commonPool() starvation in containers
 * - getAsyncExecutor: @Async for processAsync (multiple stories in parallel)
 * - triggerPipelineExecutor: admin trigger (sync path) runs in background, returns immediately
 */
@Configuration
class NarrationAsyncConfig(
    private val appProperties: AppProperties
) : AsyncConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    /** For @Async processAsync: run multiple story pipelines in parallel (bounded pool). */
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 12
            setQueueCapacity(50)
            setThreadNamePrefix("story-pipeline-")
            setRejectedExecutionHandler { _, e ->
                log.error(
                    "Story pipeline executor rejected task (queue full). active={} poolSize={}",
                    e.activeCount,
                    e.poolSize
                )
                throw RejectedExecutionException("Story pipeline executor overloaded")
            }
            initialize()
        }
        log.info("Story pipeline async executor: core=4 max=12 (parallel stories)")
        return executor
    }

    /** For admin sync trigger: run processSync in background so API returns immediately. */
    @Bean(name = ["triggerPipelineExecutor"])
    fun triggerPipelineExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            // Bulk submit can enqueue one processSync per story (up to 25); 32 avoids overflow to caller thread.
            setQueueCapacity(32)
            setThreadNamePrefix("trigger-pipeline-")
            setRejectedExecutionHandler { _, e ->
                log.error(
                    "Trigger pipeline executor rejected task (queue full). active={} poolSize={}",
                    e.activeCount,
                    e.poolSize
                )
                throw RejectedExecutionException("Trigger pipeline executor overloaded")
            }
            // Graceful shutdown: wait for in-flight pipeline tasks (up to 15 min) before stopping
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(900)
            initialize()
        }
        log.info("Trigger pipeline executor: for admin sync (background), graceful shutdown 900s")
        return executor
    }

    /** For parallel language processing within a story. Size from translation-pipeline.parallelism. */
    @Bean(name = ["pipelineLanguageExecutor"])
    fun pipelineLanguageExecutor(): ExecutorService {
        val size = appProperties.translationPipeline.parallelism.coerceIn(2, 8)
        val executor = ThreadPoolExecutor(
            size, size, 60L, TimeUnit.SECONDS,
            LinkedBlockingQueue(32),
            java.util.concurrent.Executors.defaultThreadFactory()
        )
        log.info("Pipeline language executor: {} threads (parallel langs per story)", size)
        return executor
    }

    /** Dedicated pool for pipeline rewrite (OpenAI). ForkJoinPool.commonPool() can starve in containers
     * (low CPU, shared with other async work). Size = parallelism so each language gets a thread. */
    @Bean(name = ["pipelineRewriteExecutor"])
    fun pipelineRewriteExecutor(): ExecutorService {
        val size = appProperties.translationPipeline.parallelism.coerceIn(2, 8)
        val executor = ThreadPoolExecutor(
            size, size, 60L, TimeUnit.SECONDS,
            LinkedBlockingQueue(32),
            java.util.concurrent.Executors.defaultThreadFactory()
        )
        log.info("Pipeline rewrite executor: {} threads (avoids FJP starvation)", size)
        return executor
    }

    /** Pool of threads for pipeline TTS. Was single-threaded (bottleneck); now configurable (default 4) for ~4x throughput.
     * Stays under app.narration.max-concurrent-tts to respect TTS API rate limits. */
    @Bean(name = ["pipelineTtsExecutor"])
    fun pipelineTtsExecutor(): ExecutorService {
        val size = appProperties.translationPipeline.ttsPoolSize.coerceIn(1, 8)
        val executor = ThreadPoolExecutor(
            size, size, 60L, TimeUnit.SECONDS,
            LinkedBlockingQueue(64),
            java.util.concurrent.Executors.defaultThreadFactory()
        )
        log.info("Pipeline TTS executor: {} threads (was 1; avoid single-thread bottleneck)", size)
        return executor
    }

    @Bean(name = ["narrationTtsExecutor"])
    fun narrationTtsExecutor(): ExecutorService {
        val props = appProperties.narration
        val queue = LinkedBlockingQueue<Runnable>(props.asyncQueueCapacity)
        val executor = ThreadPoolExecutor(
            props.asyncExecutorCoreSize,
            props.asyncExecutorMaxSize,
            60L, TimeUnit.SECONDS,
            queue,
            java.util.concurrent.Executors.defaultThreadFactory()
        ).apply {
            setRejectedExecutionHandler { _, e ->
                log.error(
                    "Narration TTS executor rejected task (queue full). active={} poolSize={}",
                    e.activeCount,
                    e.poolSize
                )
                throw RejectedExecutionException("Narration TTS executor overloaded")
            }
        }
        log.info("Narration TTS executor: core={} max={} queue={}",
            props.asyncExecutorCoreSize, props.asyncExecutorMaxSize, props.asyncQueueCapacity)
        return executor
    }
}
