package be.twofold.place;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class FileProcessor {

    private final List<Path> filesToProcess;

    public FileProcessor(Path root) {
        this.filesToProcess = scanForFiles(root);
    }

    private List<Path> scanForFiles(Path root) {
        try (Stream<Path> list = Files.list(root)) {
            return list
                .filter(path -> path.getFileName().toString().endsWith(".gzip"))
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void process(Consumer<Stream<String>> consumer) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        try {
            forkJoinPool.submit(() -> consumer
                .accept(filesToProcess.stream()
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
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))
                .lines()
                .skip(1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
