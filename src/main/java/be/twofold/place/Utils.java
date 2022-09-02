package be.twofold.place;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static <T> void writeAll(Path outputPath, Collection<T> collection) {
        writeAll(outputPath, collection, Objects::toString);
    }

    public static <T> void writeAll(Path outputPath, Collection<T> collection, Function<? super T, String> mapper) {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            collection.stream()
                .map(mapper)
                .forEach(line -> {
                    try {
                        writer.write(line);
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
