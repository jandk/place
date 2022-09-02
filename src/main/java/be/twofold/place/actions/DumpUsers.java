package be.twofold.place.actions;

import be.twofold.place.model.ByteArray;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DumpUsers implements Consumer<Stream<String>> {

    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();

    private final Path root;

    DumpUsers(Path root) {
        this.root = Objects.requireNonNull(root);
    }

    @Override
    public void accept(Stream<String> stream) {
        List<ByteArray> users = stream
            .map(this::parseUserId)
            .distinct().sorted()
            .collect(Collectors.toList());

        System.out.println("Dumping to file...");
        try (BufferedWriter writer = Files.newBufferedWriter(root.resolve("users.txt"))) {
            for (ByteArray user : users) {
                writer.write(encoder.encodeToString(user.getArray()));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ByteArray parseUserId(String s) {
        int start = s.indexOf(',') + 1;
        int end = s.indexOf(',', start);
        String base64 = s.substring(start, end);
        return new ByteArray(decoder.decode(base64));
    }

}
