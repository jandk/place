package be.twofold.place;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class FileProcessor {

    private final List<Path> sourceFiles;

    public FileProcessor(List<Path> sourceFiles) {
        this.sourceFiles = List.copyOf(sourceFiles);
    }

    public void process(Consumer<Stream<String>> consumer) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
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
