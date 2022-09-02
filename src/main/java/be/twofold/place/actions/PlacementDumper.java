package be.twofold.place.actions;

import be.twofold.place.Utils;
import be.twofold.place.model.ByteArray;
import be.twofold.place.model.Placement;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlacementDumper implements Consumer<Stream<String>> {

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

    private final List<String> mods = new ArrayList<>();

    private final Path placementsPath;
    private final Map<ByteArray, Integer> users;

    public PlacementDumper(Path placementsPath, Map<ByteArray, Integer> users) {
        this.placementsPath = Objects.requireNonNull(placementsPath);
        this.users = Objects.requireNonNull(users);
    }

    @Override
    public void accept(Stream<String> stream) {
        List<Placement> placements = stream
            .map(this::parseOriginalPlacement)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(Placement::getTimestamp))
            .collect(Collectors.toList());

        Utils.writeAll(placementsPath, placements);
    }

    public void dumpMods(Path modsPath) {
        Utils.writeAll(modsPath, mods);
    }


    private Placement parseOriginalPlacement(String s) {
        String[] split = s.split(",", 4);
        String[] coords = split[3]
            .substring(1, split[3].length() - 1)
            .split(",");

        if (coords.length > 2) {
            mods.add(s);
            return null;
        }

        long ts = parseDate(split[0]);
        int user = users.get(new ByteArray(Base64.getDecoder().decode(split[1])));
        int x = Integer.parseInt(coords[0]);
        int y = Integer.parseInt(coords[1]);
        int color = ColorIndex.get(split[2]);
        return new Placement(ts, user, x, y, color);
    }

    private long parseDate(String s) {
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

    private int parseNano(String s) {
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
