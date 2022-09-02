package be.twofold.place.actions;

import be.twofold.place.model.ByteArray;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserReader implements Supplier<Map<ByteArray, Integer>> {

    private final Base64.Decoder decoder = Base64.getDecoder();

    private final Path path;

    public UserReader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public Map<ByteArray, Integer> get() {
        System.out.println("Reading users into memory...");

        try (Stream<String> lines = Files.lines(path)) {
            AtomicInteger counter = new AtomicInteger();
            return lines
                .map(s -> new ByteArray(decoder.decode(s)))
                .collect(Collectors.toUnmodifiableMap(Function.identity(), __ -> counter.getAndIncrement()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
