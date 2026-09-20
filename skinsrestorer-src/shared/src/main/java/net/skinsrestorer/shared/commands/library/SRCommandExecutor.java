/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.commands.library;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the thread pool that Cloud uses to schedule command parsing, post-processing and command
 * execution for this plugin instance.
 *
 * <p>SkinsRestorer used to hand Cloud the JVM wide {@link java.util.concurrent.ForkJoinPool#commonPool()}
 * (via {@code ExecutionCoordinator#asyncCoordinator()} / {@code commonPoolExecutor()}). That pool is
 * shared with the entire JVM - the server itself, the parallel streams and {@code CompletableFuture}s
 * of every other plugin - and its parallelism is only {@code availableProcessors() - 1}, which is
 * {@code 1} on small container/VPS hosts such as Pterodactyl.</p>
 *
 * <p>Command handlers are allowed to block for a long time: MineSkin requests retry with 90 second
 * timeouts, {@code Thread.sleep(...)} is used to honour API rate limits and storage access is
 * synchronous. A single blocked handler was therefore able to occupy the only common pool worker
 * indefinitely. Cloud schedules <em>all</em> parsing and execution stages on that same pool and the
 * Bukkit command wrapper discards the returned future, so every later SkinsRestorer command simply
 * did nothing: no output, no exception, no "Unknown command" - until the server was restarted.</p>
 *
 * <p>The pool created here is owned by SkinsRestorer, bounded and self-healing:</p>
 * <ul>
 *     <li>blocked handlers cannot starve unrelated commands (up to {@value #MAX_THREADS} threads),</li>
 *     <li>idle threads terminate after {@value #KEEP_ALIVE_SECONDS} seconds,</li>
 *     <li>if a worker thread dies, the pool replaces it, and the throwable is logged with a full
 *     stack trace instead of disappearing,</li>
 *     <li>when the pool is saturated the task runs on the calling thread rather than being dropped,
 *     so a command can never fail silently,</li>
 *     <li>a task that takes suspiciously long is reported, making stalls visible in the console.</li>
 * </ul>
 */
public class SRCommandExecutor {
    private static final int MAX_THREADS = 64;
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static final long SLOW_TASK_WARN_SECONDS = 15L;

    private final SRLogger logger;
    private final ThreadPoolExecutor executor;

    public SRCommandExecutor(SRLogger logger) {
        this.logger = logger;
        this.executor = new ThreadPoolExecutor(
                0,
                MAX_THREADS,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                // No queue: a thread is created for every task without an idle worker, so a blocked
                // handler can never delay a command that was submitted after it.
                new SynchronousQueue<>(),
                new CommandThreadFactory(logger),
                new SaturatedTaskRunner(logger)
        );
    }

    /**
     * The executor to hand to Cloud's {@code ExecutionCoordinator}.
     */
    public Executor asExecutor() {
        return runnable -> executor.execute(new WatchedTask(logger, runnable));
    }

    /**
     * Stops the pool. Already submitted tasks are still completed.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Reports command tasks that take an unexpected amount of time, so a stalled command pipeline is
     * never silent again.
     */
    @RequiredArgsConstructor
    private static final class WatchedTask implements Runnable {
        private final SRLogger logger;
        private final Runnable delegate;

        @Override
        public void run() {
            long start = System.nanoTime();
            try {
                delegate.run();
            } finally {
                long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
                if (seconds >= SLOW_TASK_WARN_SECONDS) {
                    logger.warning("A SkinsRestorer command task took %d seconds to complete. This is usually caused by a slow or unreachable API request (MineSkin, Mojang, Eclipse, Ely.by) or by a slow skin storage. Other commands are not blocked by this and are still being executed.".formatted(seconds));
                }
            }
        }
    }

    /**
     * Creates daemon threads that log every uncaught throwable with a full stack trace. Bukkit's
     * plugin logger never sees exceptions thrown outside of a scheduled task, which is why a dead
     * executor was previously invisible in the console.
     */
    @RequiredArgsConstructor
    private static final class CommandThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
        private final SRLogger logger;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "SkinsRestorer-Command-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, throwable) ->
                    logger.severe("Uncaught exception in %s. The worker thread has been replaced automatically, commands keep working.".formatted(failedThread.getName()), throwable));
            return thread;
        }
    }

    /**
     * Last resort when every worker is busy. Running the task on the calling thread keeps the command
     * working instead of leaving the sender without any response, and the loud log line tells the
     * server owner what happened.
     */
    @RequiredArgsConstructor
    private static final class SaturatedTaskRunner implements RejectedExecutionHandler {
        private final SRLogger logger;

        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            logger.warning("All %d SkinsRestorer command threads are busy, running this command on the calling thread instead. If this happens often, a skin API request is likely hanging - enable debug mode in plugins/SkinsRestorer/config.yml to see the requests.".formatted(MAX_THREADS));
            runnable.run();
        }
    }
}
