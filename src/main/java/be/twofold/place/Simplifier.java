package be.twofold.place;

import be.twofold.place.model.ByteArray;
import be.twofold.place.model.Placement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Simplifier {
    private static final Map<String, Integer> ColorIndex = Map.ofEntries(
        Map.entry("#FFFFFF", 0),
        Map.entry("#D4D7D9", 1),
        Map.entry("#898D90", 2),
        Map.entry("#000000", 3),
        Map.entry("#9C6926", 4),
        Map.entry("#FF99AA", 5),
        Map.entry("#B44AC0", 6),
        Map.entry("#811E9F", 7),
        Map.entry("#51E9F4", 8),
        Map.entry("#3690EA", 9),
        Map.entry("#2450A4", 10),
        Map.entry("#7EED56", 11),
        Map.entry("#00A368", 12),
        Map.entry("#FFD635", 13),
        Map.entry("#FFA800", 14),
        Map.entry("#FF4500", 15),

        Map.entry("#6D482F", 16),
        Map.entry("#FF3881", 17),
        Map.entry("#6A5CFF", 18),
        Map.entry("#493AC1", 19),
        Map.entry("#009EAA", 20),
        Map.entry("#00756F", 21),
        Map.entry("#00CC78", 22),
        Map.entry("#BE0039", 23),

        Map.entry("#515252", 24),
        Map.entry("#FFB470", 25),
        Map.entry("#DE107F", 26),
        Map.entry("#E4ABFF", 27),
        Map.entry("#94B3FF", 28),
        Map.entry("#00CCC0", 29),
        Map.entry("#FFF8B8", 30),
        Map.entry("#6D001A", 31)
    );

    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private final List<Path> sourceFiles;
    private final Path usersPath;
    private final Path placementsPath;
    private final Path modsPath;

    private final Function<String, Placement> placementParser;

    private final List<String> mods = new ArrayList<>();
    private Map<ByteArray, Integer> users;

    Simplifier(List<Path> sourceFiles, Path targetDirectory, Year year) throws IOException {
        Objects.requireNonNull(targetDirectory);
        this.sourceFiles = List.copyOf(sourceFiles);

        Files.createDirectories(targetDirectory);
        this.usersPath = targetDirectory.resolve("users.txt");
        this.placementsPath = targetDirectory.resolve("placements.txt");
        this.modsPath = targetDirectory.resolve("mods.txt");

        this.placementParser = year.getValue() == 2017
            ? this::parsePlacement2017
            : this::parsePlacement2022;
    }

    void simplify() {
        System.out.println("Simplifying...");

        FileProcessor processor = new FileProcessor(sourceFiles);

        // Dump all the users in a separate file
        if (!Files.exists(usersPath)) {
            System.out.println("Dumping users");
            processor.process(this::dumpUsers);
        }

        if (!Files.exists(placementsPath)) {
            // Read all the users back in
            System.out.println("Reading users back in");
            users = readUsers();

            // Dump all sorted placements
            System.out.println("Dumping sorted placements");
            processor.process(this::dumpPlacements);
        }

        // Dump all mods if they don't exist
        if (!Files.exists(modsPath)) {
            System.out.println("Dumping mods");
            dumpMods(modsPath);
        }

        System.out.println("Simplifying done");
    }

    private void dumpUsers(Stream<String> stream) {
        List<ByteArray> users = stream
            .map(this::parseUserId)
            .distinct().sorted()
            .collect(Collectors.toList());

        System.out.println("Dumping to file...");
        writeAll(usersPath, users, u -> encoder.encodeToString(u.getArray()));
    }

    private Map<ByteArray, Integer> readUsers() {
        try (Stream<String> lines = Files.lines(usersPath)) {
            AtomicInteger counter = new AtomicInteger();
            return lines
                .map(s -> new ByteArray(decoder.decode(s)))
                .collect(Collectors.toUnmodifiableMap(Function.identity(), __ -> counter.getAndIncrement()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void dumpPlacements(Stream<String> stream) {
        List<Placement> placements = stream
            .map(placementParser)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(Placement::getTimestamp))
            .collect(Collectors.toList());

        writeAll(placementsPath, placements, Objects::toString);
    }

    public void dumpMods(Path modsPath) {
        writeAll(modsPath, mods, Objects::toString);
    }


    private ByteArray parseUserId(String s) {
        int start = s.indexOf(',') + 1;
        int end = s.indexOf(',', start);
        String base64 = s.substring(start, end);
        return new ByteArray(decoder.decode(base64));
    }

    private Placement parsePlacement2017(String s) {
        int i1 = s.indexOf(',');
        int i2 = s.indexOf(',', i1 + 1);
        int i3 = s.indexOf(',', i2 + 1);
        int i4 = s.indexOf(',', i3 + 1);

        // Skip invalid lines
        if (i4 == i3 + 1 || i3 == i2 + 1) {
            return null;
        }

        long ts = parseDate(s.substring(0, i1));
        int user = users.get(new ByteArray(decoder.decode(s.substring(i1 + 1, i2))));
        int x = Integer.parseInt(s, i2 + 1, i3, 10);
        int y = Integer.parseInt(s, i3 + 1, i4, 10);
        int color = Integer.parseInt(s, i4 + 1, s.length(), 10);
        return new Placement(ts, user, x, y, color);
    }

    private Placement parsePlacement2022(String s) {
        int i1 = s.indexOf(',');
        int i2 = s.indexOf(',', i1 + 1);
        int i3 = s.indexOf(',', i2 + 1);
        int i4 = s.indexOf(',', i3 + 1);

        // Mod lines have more than 4 commas
        if (s.indexOf(',', i4 + 1) != -1) {
            mods.add(s);
            return null;
        }

        long ts = parseDate(s.substring(0, i1));
        int user = users.get(new ByteArray(decoder.decode(s.substring(i1 + 1, i2))));
        int color = ColorIndex.get(s.substring(i2 + 1, i3));
        int x = Integer.parseInt(s, i3 + 2, i4, 10);
        int y = Integer.parseInt(s, i4 + 1, s.length() - 1, 10);
        return new Placement(ts, user, x, y, color);
    }

    private static long parseDate(String s) {
        int year = Integer.parseInt(s, 0, 4, 10);
        int month = Integer.parseInt(s, 5, 7, 10);
        int dayOfMonth = Integer.parseInt(s, 8, 10, 10);
        int hour = Integer.parseInt(s, 11, 13, 10);
        int minute = Integer.parseInt(s, 14, 16, 10);
        int seconds = Integer.parseInt(s, 17, 19, 10);
        int nanoOfSecond = parseNanoOfSecond(s);

        return LocalDateTime
            .of(year, month, dayOfMonth, hour, minute, seconds, nanoOfSecond)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    private static int parseNanoOfSecond(String s) {
        if (s.charAt(19) != '.') {
            return 0;
        }
        int end = s.indexOf(' ', 20);
        int fraction = Integer.parseInt(s, 20, end, 10);
        int size = end - 20;
        switch (size) {
            case 1:
                return fraction * 100_000_000;
            case 2:
                return fraction * 10_000_000;
            case 3:
                return fraction * 1_000_000;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static <T> void writeAll(Path outputPath, Collection<T> collection, Function<? super T, String> mapper) {
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
