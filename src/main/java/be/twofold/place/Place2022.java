package be.twofold.place;

import be.twofold.place.model.ByteArray;
import be.twofold.place.model.Placement;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Place2022 {
    private static final Path Root = Paths.get("C:\\Temp");

    private static final List<Color> Colors = List.of(
        new Color(0xFF, 0xFF, 0xFF),
        new Color(0xD4, 0xD7, 0xD9),
        new Color(0x89, 0x8D, 0x90),
        new Color(0x00, 0x00, 0x00),
        new Color(0x9C, 0x69, 0x26),
        new Color(0xFF, 0x99, 0xAA),
        new Color(0xB4, 0x4A, 0xC0),
        new Color(0x81, 0x1E, 0x9F),
        new Color(0x51, 0xE9, 0xF4),
        new Color(0x36, 0x90, 0xEA),
        new Color(0x24, 0x50, 0xA4),
        new Color(0x7E, 0xED, 0x56),
        new Color(0x00, 0xA3, 0x68),
        new Color(0xFF, 0xD6, 0x35),
        new Color(0xFF, 0xA8, 0x00),
        new Color(0xFF, 0x45, 0x00),

        new Color(0x6D, 0x48, 0x2F),
        new Color(0xFF, 0x38, 0x81),
        new Color(0x6A, 0x5C, 0xFF),
        new Color(0x49, 0x3A, 0xC1),
        new Color(0x00, 0x9E, 0xAA),
        new Color(0x00, 0x75, 0x6F),
        new Color(0x00, 0xCC, 0x78),
        new Color(0xBE, 0x00, 0x39),

        new Color(0x51, 0x52, 0x52),
        new Color(0xFF, 0xB4, 0x70),
        new Color(0xDE, 0x10, 0x7F),
        new Color(0xE4, 0xAB, 0xFF),
        new Color(0x94, 0xB3, 0xFF),
        new Color(0x00, 0xCC, 0xC0),
        new Color(0xFF, 0xF8, 0xB8),
        new Color(0x6D, 0x00, 0x1A)
    );

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

    private static Map<ByteArray, Integer> Users;
    private static final List<String> Mods = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        renderFrames();
    }

    private static void renderFrames() throws IOException {
        Path framesPath = Root.resolve("frames");
        Renderer renderer = new Renderer(Colors, framesPath);

        Files.lines(Root.resolve("placements.txt"))
            .map(Place2022::parsePlacement)
            .forEach(renderer::accept);
    }

    private static Placement parsePlacement(String s) {
        int i1 = s.indexOf(',');
        int i2 = s.indexOf(',', i1 + 1);
        int i3 = s.indexOf(',', i2 + 1);
        int i4 = s.indexOf(',', i3 + 1);

        return new Placement(
            Long.parseLong(s, 0, i1, 10),
            Integer.parseInt(s, i1 + 1, i2, 10),
            Integer.parseInt(s, i2 + 1, i3, 10),
            Integer.parseInt(s, i3 + 1, i4, 10),
            Integer.parseInt(s, i4 + 1, s.length(), 10)
        );
    }

    private static void simplify() throws IOException {
        System.out.println("Dumping users...");
        readAndProcess(paths, Place2022::dumpUsers);

//        System.out.println("Reading users...");
//        Users = readUsers();

        System.out.println("Dumping sorted placements...");
        readAndProcess(paths, Place2022::dumpSorted);

        System.out.println("Dumping mods");
        dumpMods();
    }

    private static void dumpColor(Color color) {
        System.out.printf("new Color(0x%02x, 0x%02x, 0x%02x),%n", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void dumpUsers(Stream<String> stream) {
        List<ByteArray> users = stream
            .map(s -> {
                int start = s.indexOf(',') + 1;
                int end = s.indexOf(',', start);
                return new ByteArray(Base64.getDecoder().decode(s.substring(start, end)));
            })
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        System.out.println("Dumping to file...");
        try (BufferedWriter writer = Files.newBufferedWriter(Root.resolve("users.txt"))) {
            for (ByteArray user : users) {
                writer.write(Base64.getEncoder().encodeToString(user.array()));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("Converting to Users map...");
        Users = IntStream.range(0, users.size())
            .boxed()
            .collect(Collectors.toUnmodifiableMap(users::get, Function.identity()));
    }

    private static void dumpSorted(Stream<String> stream) {
        List<Placement> placements = stream
            .map(Place2022::parseOriginalPlacement)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(Placement::getTs))
            .collect(Collectors.toList());

        try (BufferedWriter writer = Files.newBufferedWriter(Root.resolve("placements.txt"))) {
            for (Placement placement : placements) {
                writer.write(placement.toString());
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void dumpMods() {
        try (BufferedWriter writer = Files.newBufferedWriter(Root.resolve("mods.txt"))) {
            for (String mod : Mods) {
                writer.write(mod);
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private static void readAndProcess(List<Path> paths, Consumer<Stream<String>> consumer) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(12);
        try {
            forkJoinPool.submit(() -> consumer
                .accept(paths.stream()
                    .parallel()
                    .flatMap(Place2022::readFile)
                )
            ).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private static Stream<String> readFile(Path path) {
        System.out.println("Reading file " + path);
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))
            .lines()
            .skip(1);
    }

    private static Placement parseOriginalPlacement(String s) {
        String[] split = s.split(",", 4);
        String[] coords = split[3]
            .substring(1, split[3].length() - 1)
            .split(",");

        if (coords.length > 2) {
            Mods.add(s);
            return null;
        }

        long ts = parseDate(split[0]);
        int user = Users.get(new ByteArray(Base64.getDecoder().decode(split[1])));
        int x = Integer.parseInt(coords[0]);
        int y = Integer.parseInt(coords[1]);
        int color = ColorIndex.get(split[2]);
        return new Placement(ts, user, x, y, color);
    }

    private static long parseDate(String s) {
        int year = Integer.parseInt(s, 0, 4, 10);
        int month = Integer.parseInt(s, 5, 7, 10);
        int dayOfMonth = Integer.parseInt(s, 8, 10, 10);
        int hour = Integer.parseInt(s, 11, 13, 10);
        int minute = Integer.parseInt(s, 14, 16, 10);
        int seconds = Integer.parseInt(s, 17, 19, 10);
        int nano = parseNano(s);

        return LocalDateTime
            .of(year, month, dayOfMonth, hour, minute, seconds, nano)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    private static int parseNano(String s) {
        if (s.charAt(19) != '.') {
            return 0;
        }
        int end = s.indexOf(' ', 20);
        int fraction = Integer.parseInt(s, 20, end, 10);
        if (end - 20 == 3) {
            return fraction * 1_000_000;
        }
        if (end - 20 == 2) {
            return fraction * 10_000_000;
        }
        if (end - 20 == 1) {
            return fraction * 100_000_000;
        }
        throw new IllegalArgumentException();
    }
}
