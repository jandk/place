package be.twofold.place;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
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

    public static IndexColorModel fromColors(List<Color> colors) {
        byte[] r = new byte[colors.size()];
        byte[] g = new byte[colors.size()];
        byte[] b = new byte[colors.size()];

        for (int i = 0; i < colors.size(); i++) {
            r[i] = (byte) colors.get(i).getRed();
            g[i] = (byte) colors.get(i).getGreen();
            b[i] = (byte) colors.get(i).getBlue();
        }
        return new IndexColorModel(colors.size() > 16 ? 5 : 4, colors.size(), r, g, b);
    }

    public static long parseDate(String s) {
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
}
