package be.twofold.place.actions;

import be.twofold.place.Utils;
import be.twofold.place.model.ByteArray;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserDumper implements Consumer<Stream<String>> {

    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();

    private final Path usersPath;

    public UserDumper(Path usersPath) {
        this.usersPath = Objects.requireNonNull(usersPath);
    }

    @Override
    public void accept(Stream<String> stream) {
        List<ByteArray> users = stream
            .map(this::parseUserId)
            .distinct().sorted()
            .collect(Collectors.toList());

        System.out.println("Dumping to file...");
        Utils.writeAll(usersPath, users, u -> encoder.encodeToString(u.getArray()));
    }

    private ByteArray parseUserId(String s) {
        int start = s.indexOf(',') + 1;
        int end = s.indexOf(',', start);
        String base64 = s.substring(start, end);
        return new ByteArray(decoder.decode(base64));
    }

}
