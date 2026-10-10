package com.northwind.tools.docs;

import com.embabel.agent.api.annotation.LlmTool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Tool 9: search NorthWind's policy documents. Confined to one directory: the model chooses
 * search terms and file NAMES, never paths, so it cannot read anything else on the machine.
 */
public class FileSearchTools {

    public record Match(String file, String line) {
    }

    private final Path root;

    public FileSearchTools(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @LlmTool(description = "Search NorthWind policy documents for a phrase; returns up to 5 matching lines")
    public List<Match> searchPolicies(@LlmTool.Param(description = "Phrase to search for, e.g. return window") String phrase) {
        String needle = phrase.toLowerCase(Locale.ROOT).strip();
        if (needle.length() < 3) {
            return List.of();
        }
        List<Match> matches = new ArrayList<>();
        try (Stream<Path> files = Files.list(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".md")).sorted().toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.toLowerCase(Locale.ROOT).contains(needle)) {
                        matches.add(new Match(file.getFileName().toString(), line.strip()));
                        if (matches.size() == 5) {
                            return matches;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return matches;
    }

    @LlmTool(description = "Read a whole policy document by its file name, e.g. returns.md")
    public String readPolicy(@LlmTool.Param(description = "File name only, e.g. returns.md") String fileName) {
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !fileName.endsWith(".md")) {      // blocks ../../etc/passwd
            return "Only policy documents in the policy library can be read.";
        }
        try {
            return Files.exists(file) ? Files.readString(file) : "No such policy document.";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
