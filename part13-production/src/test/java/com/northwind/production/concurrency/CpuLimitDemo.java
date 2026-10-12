package com.northwind.production.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Chapter 96: run in a separate JVM with -XX:ActiveProcessorCount=N to see what a container's CPU limit does
 * to code that relies on the JVM's shared pools. Each task waits 300 ms, as a model or HTTP call would.
 */
public final class CpuLimitDemo {

    private CpuLimitDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("availableProcessors=" + Runtime.getRuntime().availableProcessors()
                + " commonPool.parallelism=" + ForkJoinPool.commonPool().getParallelism());
        report("parallel stream", () -> {
            IntStream.range(0, 4).parallel().forEach(i -> pause());
            return null;
        });
        report("ForkJoinPool.commonPool()", () -> {
            IntStream.range(0, 4).mapToObj(i -> ForkJoinPool.commonPool().submit(CpuLimitDemo::pause)).toList()
                    .forEach(java.util.concurrent.ForkJoinTask::join);
            return null;
        });
        report("CompletableFuture.supplyAsync, no executor", () -> {
            IntStream.range(0, 4).mapToObj(i -> CompletableFuture.runAsync(CpuLimitDemo::pause)).toList()
                    .forEach(CompletableFuture::join);
            return null;
        });
        try (ExecutorService executor = Executors.newCachedThreadPool()) {    // like Embabel's own executor
            report("own unbounded executor", () -> {
                IntStream.range(0, 4).mapToObj(i -> CompletableFuture.runAsync(CpuLimitDemo::pause, executor)).toList()
                        .forEach(CompletableFuture::join);
                return null;
            });
        }
    }

    private static void report(String what, Supplier<Void> work) {
        long start = System.nanoTime();
        work.get();
        long ms = (System.nanoTime() - start) / 1_000_000;
        long rounds = Math.round(ms / 300.0);                               // 1 round = all four at once
        System.out.println("  4 x 300 ms via " + what + ": " + rounds + (rounds == 1 ? " round" : " rounds"));
    }

    private static void pause() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
