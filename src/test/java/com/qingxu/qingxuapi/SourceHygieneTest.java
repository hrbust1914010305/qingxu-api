package com.qingxu.qingxuapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceHygieneTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final List<String> MOJIBAKE_MARKERS = List.of(
            "\u93c0", "\u93c3", "\u9427", "\u9422", "\u95ab", "\u7ecb",
            "\u7035", "\u9983", "\u9241", "\u923f", "\u9234", "\u93b5",
            "\u95ae", "\u95c2", "\u7ecc", "\u951b", "\u9286", "\u9359",
            "\u947e", "\u5d1f", "\u6960", "\ufffd"
    );

    @Test
    void productionCodeDoesNotContainTemporaryConsoleDebugging() throws IOException {
        assertNoSourceContains("System.out.");
        assertNoSourceContains("System.err.");
        assertNoSourceContains("log.debug(");
        assertNoSourceContains("log.trace(");
    }

    @Test
    void productionCodeDoesNotContainMojibakeChineseText() throws IOException {
        for (String marker : MOJIBAKE_MARKERS) {
            assertNoSourceContains(marker);
        }
    }

    private static void assertNoSourceContains(String text) throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            List<String> matches = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> matchingLines(path, text).stream())
                    .toList();

            assertThat(matches)
                    .as("Unexpected source text: %s", text)
                    .isEmpty();
        }
    }

    private static List<String> matchingLines(Path path, String text) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return java.util.stream.IntStream.range(0, lines.size())
                    .filter(index -> lines.get(index).contains(text))
                    .mapToObj(index -> path + ":" + (index + 1) + " " + lines.get(index).trim())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
