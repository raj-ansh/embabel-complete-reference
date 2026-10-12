package com.northwind.production.concurrency;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 96: runs {@link CpuLimitDemo} in child JVMs (the same Java 25 as the tests) that believe they have
 * 1 or 3 CPUs, which is what Java 25 reports inside a container limited to that many CPUs.
 */
class CpuLimitTest {

    private static String run(int cpus) throws IOException, InterruptedException {
        String java = ProcessHandle.current().info().command().orElse("java");
        Process process = new ProcessBuilder(java, "-XX:ActiveProcessorCount=" + cpus,
                "-cp", Path.of("target", "test-classes").toString(), CpuLimitDemo.class.getName())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output.strip();
    }

    @Test
    void sharedPoolsShrinkWithTheCpuLimit() throws Exception {
        System.out.println("java " + Runtime.version().feature() + ", child JVM limited to 1 CPU:\n" + run(1));
        String three = run(3);
        System.out.println("child JVM limited to 3 CPUs:\n" + three);
        assertThat(three).contains("own unbounded executor: 1 round");
    }
}
