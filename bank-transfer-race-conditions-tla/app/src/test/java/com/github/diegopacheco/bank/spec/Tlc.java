package com.github.diegopacheco.bank.spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class Tlc {

    private static final Path SPEC_DIR = Path.of("..", "spec").toAbsolutePath().normalize();
    private static final Path JAR = Path.of("..", ".tools", "tla2tools.jar").toAbsolutePath().normalize();
    private static final Path LOGS = Path.of("target", "tlc").toAbsolutePath();

    private Tlc() {
    }

    static TlcResult check(String spec, String config) throws IOException, InterruptedException {
        if (!Files.exists(JAR)) {
            throw new IllegalStateException("tla2tools.jar not found at " + JAR + ", run ./scripts/setup.sh");
        }
        Files.createDirectories(LOGS);
        Path log = LOGS.resolve(config + ".log");
        Path states = Files.createTempDirectory("tlc-" + config);
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(List.of(
                java, "-XX:+UseParallelGC", "-cp", JAR.toString(), "tlc2.TLC",
                "-workers", "1", "-noGenerateSpecTE", "-metadir", states.toString(),
                "-config", config + ".cfg", spec + ".tla"))
                .directory(SPEC_DIR.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        int exitCode = process.waitFor();
        return new TlcResult(exitCode, Files.readString(log, StandardCharsets.UTF_8), log);
    }
}
