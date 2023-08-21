package be.twofold.place;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.zip.*;

public final class FileProcessor {

    private final List<Path> sourceFiles;

    public FileProcessor(List<Path> sourceFiles) {
        this.sourceFiles = List.copyOf(sourceFiles);
    }

    public void process(Consumer<Stream<String>> consumer) {
        int processors = Runtime.getRuntime().availableProcessors() / 4;
        ForkJoinPool forkJoinPool = new ForkJoinPool(processors);
        try {
            forkJoinPool.submit(() -> consumer
                .accept(sourceFiles.stream()
                    .parallel()
                    .flatMap(FileProcessor::readFile)
                )
            ).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private static Stream<String> readFile(Path path) {
        System.out.println("Reading file: " + path);
        try {
            InputStream in = path.getFileName().toString().endsWith(".gzip")
                ? new GZIPInputStream(Files.newInputStream(path))
                : Files.newInputStream(path);

            return new BufferedReader(new InputStreamReader(in))
                .lines()
                .skip(1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
