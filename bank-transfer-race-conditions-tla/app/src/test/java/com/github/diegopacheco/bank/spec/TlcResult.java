package com.github.diegopacheco.bank.spec;

import java.nio.file.Path;

record TlcResult(int exitCode, String output, Path log) {

    boolean noErrorFound() {
        return exitCode == 0 && output.contains("Model checking completed. No error has been found.");
    }

    boolean violated(String invariant) {
        return exitCode != 0 && output.contains("Error: Invariant " + invariant + " is violated.");
    }

    boolean deadlockReached() {
        return exitCode != 0 && output.contains("Error: Deadlock reached.");
    }

    String lastState() {
        int start = output.lastIndexOf("\nState ");
        int end = output.indexOf("\n\n", start + 1);
        return start < 0 ? "" : output.substring(start + 1, end < 0 ? output.length() : end);
    }

    String describe() {
        return "TLC exit " + exitCode + ", full output in " + log;
    }
}
