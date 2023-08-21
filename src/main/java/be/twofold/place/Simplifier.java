package be.twofold.place;

import be.twofold.place.model.*;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

final class Simplifier {
    private static final Map<String, Integer> ColorIndex2022 = createColorIndex(Renderer.Colors2022);
    private static final Map<String, Integer> ColorIndex2023 = createColorIndex(Renderer.Colors2023);
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private final List<Path> sourceFiles;
    private final Path usersPath;
    private final Path placementsPath;
    private final Path modsPath;

    private final Function<String, Placement> placementParser;
    private final List<String> mods = new ArrayList<>();
    private Map<ByteArray, Integer> users;

    Simplifier(List<Path> sourceFiles, Path targetDirectory, Year year) {
        Objects.requireNonNull(targetDirectory);
        this.sourceFiles = List.copyOf(sourceFiles);

        this.usersPath = targetDirectory.resolve("users.txt");
        this.placementsPath = targetDirectory.resolve("placements.txt");
        this.modsPath = targetDirectory.resolve("mods.txt");

        this.placementParser = switch (year.getValue()) {
            case 2017 -> this::parsePlacement2017;
            case 2022 -> this::parsePlacement2022;
            case 2023 -> this::parsePlacement2023;
            default -> throw new IllegalArgumentException("Year must be 2017, 2022 or 2023");
        };
    }

    private static Map<String, Integer> createColorIndex(List<Color> colorList) {
        List<String> colors = colorList.stream()
            .map(color -> String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()))
            .collect(Collectors.toList());

        return toMap(colors);
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
        if (!Files.exists(modsPath) && !mods.isEmpty()) {
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
        writeAll(usersPath, users, u -> encoder.encodeToString(u.array()));
    }

    private Map<ByteArray, Integer> readUsers() {
        try (Stream<String> lines = Files.lines(usersPath)) {
            List<ByteArray> users = lines
                .map(s -> new ByteArray(decoder.decode(s)))
                .collect(Collectors.toList());

            return toMap(users);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void dumpPlacements(Stream<String> stream) {
        List<Placement> placements = stream
            .map(placementParser)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());

        writeAll(placementsPath, placements, Objects::toString);
    }

    private void dumpMods(Path modsPath) {
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

        // Take out invalid coordinates
        short x = (short) Integer.parseInt(s, i2 + 1, i3, 10);
        short y = (short) Integer.parseInt(s, i3 + 1, i4, 10);
        if (x > 999 || y > 999) {
            return null;
        }

        long ts = parseDate(s.substring(0, i1));
        int user = users.get(new ByteArray(decoder.decode(s.substring(i1 + 1, i2))));
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
        short x = (short) Integer.parseInt(s, i3 + 2, i4, 10);
        short y = (short) Integer.parseInt(s, i4 + 1, s.length() - 1, 10);
        int color = ColorIndex2022.get(s.substring(i2 + 1, i3));
        return new Placement(ts, user, x, y, color);
    }

    private Placement parsePlacement2023(String s) {
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
        short x = (short) Integer.parseInt(s, i2 + 2, i3, 10);
        short y = (short) Integer.parseInt(s, i3 + 1, i4 - 1, 10);
        int color = ColorIndex2023.get(s.substring(i4 + 1));
        return new Placement(ts, user, x, y, color);
    }

    private long parseDate(String s) {
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

    private int parseNanoOfSecond(String s) {
        if (s.charAt(19) != '.') {
            return 0;
        }
        int end = s.indexOf(' ', 20);
        int fraction = Integer.parseInt(s, 20, end, 10);
        return switch (end - 20) {
            case 1 -> fraction * 100_000_000;
            case 2 -> fraction * 10_000_000;
            case 3 -> fraction * 1_000_000;
            default -> throw new IllegalArgumentException();
        };
    }

    private <T> void writeAll(Path outputPath, Collection<T> collection, Function<? super T, String> mapper) {
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

    private static <T> Map<T, Integer> toMap(Collection<T> collection) {
        AtomicInteger counter = new AtomicInteger();
        return collection.stream()
            .collect(Collectors.toUnmodifiableMap(
                Function.identity(),
                __ -> counter.incrementAndGet()
            ));
    }
}
